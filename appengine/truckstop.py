import cgi
import os
from django.utils import simplejson as json

import oauth
import hashlib
from datastore import *

from time import time
from datetime import datetime

import logging
from google.appengine.api import urlfetch


from google.appengine.api import users
from google.appengine.ext import webapp
from google.appengine.ext.webapp import util
from google.appengine.ext.webapp.util import run_wsgi_app
from google.appengine.ext import db
from google.appengine.ext.db import polymodel
from google.appengine.ext.db import stats
from google.appengine.ext.webapp import template

import cStringIO
import csv


from surveymodels import *

from dashboards import *





#
# Get the SDK:
# http://code.google.com/appengine/docs/python/gettingstarted/devenvironment.html
#
# If not using the gui version to run a local server to test what things look like
# cd to/the/dir that contains the appengine directory with your code, then
# dev_appserver.py appengine
# then point your browser to localhost:8080
#
# To upload the code if not using the gui, do:
# appcfg.py update appengine/
#




###########################################
# oauth stuff - taken from stress chill
###########################################


########################
# You must visit /get_consumer with your web browser and gernerate
# the key and password and put those into your phone application


# base clase to be extended by other handlers needing oauth
class BaseHandler(webapp.RequestHandler):
    def __init__(self, *args, **kwargs):
        self.oauth_server = oauth.OAuthServer(DataStore())
        self.oauth_server.add_signature_method(oauth.OAuthSignatureMethod_PLAINTEXT())
        self.oauth_server.add_signature_method(oauth.OAuthSignatureMethod_HMAC_SHA1())
    # end __init__ method

    def send_oauth_error(self, err=None):
        self.response.clear()
        if os.environ.get('HTTP_HOST'):
            base_url = os.environ['HTTP_HOST']
        else:
            base_url = os.environ['SERVER_NAME']

        realm_url = 'http://' + base_url

        header = oauth.build_authenticate_header(realm=realm_url)
        for k,v in header.iteritems():
            self.response.headers.add_header(k, v)
        self.response.set_status(401, str(err.message))
        logging.error(err.message)
    # end send_oauth_error method

    def get(self):
        self.handler()
    # end get method

    def post(self):
        self.handler()
    # end post method

    def handler(self):
        pass
    # end handler method

    # get the request
    def construct_request(self, token_type = ''):
        logging.debug('\n\n' + token_type + 'Token------')
        logging.debug(self.request.method)
        logging.debug(self.request.url)
        logging.debug(self.request.headers)

        # get any extra parameters required by server
        self.paramdict = {}
        for j in self.request.arguments():
            self.paramdict[j] = self.request.get(j)
        logging.debug('parameters received: ' +str(self.paramdict))

        # construct the oauth request from the request parameters
        try:
            oauth_request = oauth.OAuthRequest.from_request(self.request.method, self.request.url, headers=self.request.headers, parameters=self.paramdict)
            return oauth_request
        except oauthOauthError, err:
            logging.error("construct_request: Could not create oauth_request")
            self.send_oauth_error(oauth.OAuthError('could not create oauth_request'))
            self.send_oauth_error(err)
            return False

        # extra check... 
        if not oauth_request:
            self.send_oauth_error(oauth.OAuthError('could not create oauth_request'))
            return False
# End BaseHandler class

# handler for: /request_token
class RequestTokenHandler(BaseHandler):
    def handler(self):
        logging.debug("calling request_token")
        oauth_request = self.construct_request('Request')
        if not oauth_request:
            self.send_oauth_error(oauth.OAuthError('could not create oauth_request'))
            return

        try:
            token = self.oauth_server.fetch_request_token(oauth_request)

            self.response.out.write(token.to_string())
            logging.debug('Request Token created')
        except oauth.OAuthError, err:
            self.send_oauth_error(err)
    # end handler method
# End RequestTokenHandler class


# handler for: /authorize
# required fields: 
# - username: string 
# - password: string, sha1 of plaintext password
# TODO: Change this back into a normal user authorize....
#	redirect to login page, have user authorize app, and redirect to callback if one provided...
class UserAuthorize(BaseHandler):
    def handler(self):
        oauth_request = self.construct_request('Authorize')
        if not oauth_request:
            self.send_oauth_error(oauth.OAuthError('could not create oauth_request'))
            return
    
        try:
            username = None
            password = None
            # set by construct_request
            if 'username' in self.paramdict:
                username = self.paramdict['username']
            if 'password' in self.paramdict:
                password = self.paramdict['password']

            if not username or not password:
                self.response.set_status(401, 'missing username or password')
                logging.error('missing username or password')
                return

            ukey = UserTable().check_valid_password(username, password)

            if not ukey:
                self.response.set_status(401, 'incorrect username or password')
                logging.error('incorrect username or password')
                return

            # perform user authorize
            token = self.oauth_server.fetch_request_token(oauth_request)
            token = self.oauth_server.authorize_token(token, ukey)
            logging.debug(token)

            logging.debug(token.to_string())

            self.response.out.write(token.get_callback_url())
            logging.debug(token.get_callback_url())
        except oauth.OAuthError, err:
            self.send_oauth_error(err)
    # end handler method
# End UserAuthorize Class

# handler for: /access_token 
class AccessTokenHandler(BaseHandler):
    def handler(self):
        logging.debug("calling access_token")
        oauth_request = self.construct_request('Access')
        if not oauth_request:
            self.send_oauth_error(oauth.OAuthError('could not create oauth_request'))
            return

        try:
            token = self.oauth_server.fetch_access_token(oauth_request)

            self.response.out.write(token.to_string())
        except oauth.OAuthError, err:
            self.send_oauth_error(err)
    # end handler method
# End AccessTokenHandler Class

# handler for: /authorize_access
# cheat for mobile phone so no back and forth with redirects...
# access as if fetching request token
# also send username, sha1 of password
# required fields: 
# - username: string 
# - password: string, sha1 of plaintext password
# returns access token
class AuthorizeAccessHandler(BaseHandler):
    def handler(self):
        logging.debug("Calling authorize_access")
        oauth_request = self.construct_request('AuthorizeAccess')
        logging.debug("Done calling contruct_request")
        if not oauth_request:
            self.send_oauth_error(oauth.OAuthError('could not create oauth_request'))
            logging.error("Could not create oauth_request")
            return

        try:
            # request token
            logging.debug("calling fetch request token")
            token = self.oauth_server.fetch_request_token(oauth_request)
            logging.debug('Request Token created: ' + token.to_string())

            username = None
            password = None

            # check user 
            if 'username' in self.paramdict:
                username = self.paramdict['username']
            if 'password' in self.paramdict:
                password = self.paramdict['password']

            if not username or not password:
                self.response.set_status(401, 'missing username or password')
                logging.error('missing username or password')
                return

            ukey = UserTable().check_valid_password(username, password)

            if not ukey:
                self.response.set_status(401, 'incorrect username or password')
                logging.error('incorrect username or password')
                return

            # perform user authorize
            token = self.oauth_server.authorize_token(token, ukey)
            logging.debug('Token authorized: ' + token.to_string())

            # create access token
            consumer = Consumer().get_consumer(oauth_request.get_parameter('oauth_consumer_key'))

            oauth_request.set_parameter('oauth_verifier', token.verifier)
            oauth_request.set_parameter('oauth_token', token.key)

            oauth_request.sign_request(oauth.OAuthSignatureMethod_PLAINTEXT(), consumer, token)

            logging.debug('Current OAuth Param: ' + str(oauth_request.parameters))
            token = self.oauth_server.fetch_access_token(oauth_request)

            self.response.out.write(token.to_string())
        except oauth.OAuthError, err:
            logging.error("authorize_access try fail ")
            self.send_oauth_error(err)
    # end handler method
# End AuthorizeAccessHandler Class



# handler for /create_consumer
# form to create a consumer key & setup permissions to access resources
class CreateConsumer(webapp.RequestHandler):
    def get(self):
        self.handle()
    def post(self):
        self.handle()
    def handle(self):
        self.response.out.write('''
<html>
<body>
<form action="/get_consumer" METHOD="POST">
    Select resources:
    resource 1 (read test): <input name="res1" type="checkbox" value="res1"><br />
    resource 2 (write test): <input name="res2" type="checkbox" value="res2"><br />
    resource 3 (some other resource): <input name="res3" type="checkbox" value="res3"><br />
    <input type="submit" name="submitted">
</form>
</body>
</html>
        ''')
# End CreateConsumer class

# handler for: /get_consumer
# create consumer key/secret & add to resource table
class GetConsumer(webapp.RequestHandler):
    def get(self):
        self.handle()
    def post(self):
        self.handle()
    def handle(self):
        if not self.request.get('submitted'):
            self.response.out.write('no')
            return

        allowed_res = ['res1', 'res2', 'res3']
        consumer = Consumer().insert_consumer('tester1')
        self.response.out.write('key: '+consumer.key+'<br />\n')
        self.response.out.write('pass: '+consumer.secret+'<br />\n')

        if self.request.get('res1') in allowed_res:
            if ResourceTable().create_resource(self.request.get('res1'), consumer.key):
                self.response.out.write('has permission on res 1<br />')
            else:
                self.response.out.write('could not grant on res 1<br />')
        if self.request.get('res2') in allowed_res:
            if ResourceTable().create_resource(self.request.get('res2'), consumer.key):
                self.response.out.write('has permission on res 2<br />')
            else:
                self.response.out.write('could not grant on res 2<br />')
        if self.request.get('res3') in allowed_res:
            if ResourceTable().create_resource(self.request.get('res3'), consumer.key):
                self.response.out.write('has permission on res 3<br />')
            else:
                self.response.out.write('could not grant on res 3<br />')
    # end handle
# End GetConsumer class
        

# handler for: /create_user
# form to set up new user
class CreateUser(webapp.RequestHandler):
    def get(self):
        self.handle()
    def post(self):
        self.handle()
    def handle(self):
        self.response.out.write('''
<html>
<body>
<form action="/confirm_user" METHOD="POST">
    username: <input name="username" type="text"><br />
    password: <input name="password" type="password"><br />
    confirm password: <input name="confirmpassword" type="password"><br />
    email: <input name="email" type="text"><br />
    <input type="submit">
</form>
</body>
</html>
        ''')
# End CreateUser class

# handler for: /confirm_user
# adds user
# required fields:
#	- username: string
#	- password: string
#	- confirmpassword: string - must match password
# optional:
#	- email: string
class ConfirmUser(webapp.RequestHandler):
    def post(self):
        username = self.request.get('username')
        password = self.request.get('password')
        confirmpassword = self.request.get('confirmpassword')
        email = self.request.get('email')
        if not username or not password or not confirmpassword:
            self.response.set_status(401, 'Missing field')
            logging.error('Missing field')
            return
        if password != confirmpassword:
            self.response.set_status(401, 'Password mismatch')
            logging.error('Password mismatch')
            return

        if not UserTable().create_user(username, password, email):
            self.response.set_status(401, 'could not create user')
            logging.error('could not create user')
            return

        self.response.out.write('user added')





# res1
# currently not used.  !!!
class ProtectedResourceHandler(BaseHandler):
    def handler(self):
        oauth_request = self.construct_request('Protected')
        if not oauth_request:
            self.send_oauth_error(oauth.OAuthError('could not create oauth_request'))
            return

        logging.debug(oauth_request.parameters)
        try:
            consumer, token, params = self.oauth_server.verify_request(oauth_request)

            if not ResourceTable().check_valid_consumer('res1', consumer.key):
                self.send_oauth_error(oauth.OAuthError('consumer may not access this resource'))
                return
            logging.debug('token string: '+token.to_string())

            s = HomeSurvey()

            # check user 
            if 'username' in params:
                s.username = params['username']

            if 'longitude' in params:
                s.longitude = params['longitude']

            if 'latitude' in params:
                s.latitude = params['latitude']

            if 'q_1' in params:
                s.q_1 = params['q_1']

            if 'q_2' in params:
                s.q_2 = params['q_2']

            if 'q_3' in params:
                s.q_3 = params['q_3']

            if 'time' in params:
                s.time = long(params['time'])

            if 'version' in params:
                s.version = params['version']

            if 'photo' in params:
                file_content = params['photo']
                try:
                    s.photo = db.Blob(file_content)
                except TypeError:
                    s.photo = None
            else:
                s.photo = None

            s.put()

        except oauth.OAuthError, err:
            self.send_oauth_error(err)
    # end handler method
# End ProtectedResourceHandler Class





# handler for: /protected_upload_pathtrace
# required fields:
#	- oauth_token: string containing access key
# this is a temporary hack...
class ProtectedResourceHandlerBHPathTrace(webapp.RequestHandler):
    def post(self):
        self.handle()
    def get(self):
        self.handle()

    def handle_image(self, file_content):
        if file_content:
            try:
                return db.Blob(file_content)
            except TypeError:
                return None
        else:
            return None
    
    def handle(self):
        logging.debug('\n\nProtected Resource BHPathTrace------')
        logging.debug(self.request.method)
        logging.debug(self.request.url)
        logging.debug(self.request.headers)
        
        # get any extra parameters required by server
        self.paramdict = {}
        for j in self.request.arguments():
            self.paramdict[j] = self.request.get(j)
            if j.find('i_') < 0:
                try:
                    logging.debug('parameter ' + j + ' -> ' +str(self.paramdict[j]))
                except UnicodeDecodeError:
                    logging.debug('Could not decode: ' + j + ' -> ' + self.paramdict[j])

        req_token = self.request.get('oauth_token')
        
        if req_token != '':
            try :
                t = db.GqlQuery("SELECT * FROM Token WHERE ckey = :1", req_token).get()
                if not t:
                    logging.error('if you got here, token lookup failed.')
                    self.error(401)
                    return

                #mainS = Survey()
                s = PathSurveyTrace()
                
                u = db.GqlQuery("SELECT * FROM UserTable WHERE ckey = :1", t.user).get()
                
                s.username = u.username
                s.longitude = self.request.get('longitude')
                s.latitude = self.request.get('latitude')
                thetime = self.request.get('time')
                if thetime:
                    s.time = long(thetime)
                    try:
                        s.timefull = datetime.fromtimestamp(long(thetime) / 1000.0)
                    except ValueError, e:
                        logging.error("Unable to set timeful with %s, %s", thetime, e)
                        s.timefull = None
                else:
                    s.time = 0
                    s.timefull = None
                s.version = self.request.get('version')
                #s.put()
                s.put()
                
            except db.Error, err:
                logging.error('error inserting to database' + str(type(err)) + ' ' + str(err.args) + ' ' + str(err))
                self.error(401)
                return
        else:
            logging.error('request token empty')
            self.error(401)




class ProtectedResourceHandlerUploadAll(webapp.RequestHandler):
    def post(self):
        self.handle()
    def get(self):
        self.handle()

    def handle_image2(self, file_content):
        if file_content:
            try:
                return db.Blob(file_content)
            except TypeError:
                return None
        else:
            return None

    def handle_image(self, appname, key, theid):
        file_content = self.request.get(theid)
        i = TruckImages()         #EDIT

        if file_content:
            try:
                i.image = db.Blob(file_content)
                i.image_index = theid
                i.survey_ref = key
                i.put()
                return True
            except TypeError:
                None
        return False

    def handle(self):
        logging.debug('\n\nProtected Resource ------')
        logging.debug(self.request.method)
        logging.debug(self.request.url)
        logging.debug(self.request.headers)
        
        # get any extra parameters required by server
        #self.paramdict = {}
        #for j in self.request.arguments():
        #    self.paramdict[j] = self.request.get(j)
            #if j.find('i_') < 0:
            #    try:
                    #logging.debug('parameter ' + j + ' -> ' +str(self.paramdict[j]))
            #    except UnicodeDecodeError:
                    #logging.debug('Could not decode: ' + j + ' -> ' + self.paramdict[j])

        req_token = self.request.get('oauth_token')
        
        if req_token != '':
            try :
                t = db.GqlQuery("SELECT * FROM Token WHERE ckey = :1", req_token).get()
                if not t:
                    logging.error('if you got here, token lookup failed.')
                    self.error(401)
                    return

                appname = self.request.get('appname')
                if appname == None:
                    self.error(401)
                    return

                # Figure out the DB storage type
                picksurvey = {
                    "TruckStop": lambda: TruckSurvey()     #EDIT
                    }

                s = None
                try:
                    s = picksurvey[appname]()
                except KeyError:
                    self.error(401)
                    return
                
                
                u = db.GqlQuery("SELECT * FROM UserTable WHERE ckey = :1", t.user).get()
                
                s.username = u.username
                s.longitude = self.request.get('longitude')
                s.latitude = self.request.get('latitude')
                thetime = self.request.get('time')
                if thetime:
                    s.time = long(thetime)
                    try:
                        s.timefull = datetime.fromtimestamp(long(thetime) / 1000.0)
                    except ValueError:
                        logging.error("Unable to set timeful with %s", thetime)
                        s.timefull = None
                else:
                    s.time = 0
                    s.timefull = None
                s.version = self.request.get('version')
                s.imei = self.request.get('IMEI')

                #s.put()

                # By hand? Find a way to set these automatically
                responsecount = {
                    "TruckStop": 5            #EDIT
                    }

                for i in range(responsecount[appname]):
                    rv = 'r_' + str(i)
                    resp = self.request.get(rv)
                    if resp != None:
                        if resp == "NO RESPONSE":
                            resp = "-"
                    setattr(s, rv, resp)

                s.put()
                skey = s.key()

                # By hand? Find a way to set these automatically
                imagecount = {
                    "TruckStop": 1           #EDIT
                    }

                for i in range(imagecount[appname]):
                    iv = 'i_' + str(i)
                    yesim = self.handle_image(appname, skey, iv)
                    setattr(s, iv, yesim)
                    
                s.put()

                # collect some stats
                obstime = datetime.fromtimestamp(long(thetime) / 1000.0)
                #startt = obstime.strftime("%Y/%m/%d %H:%M:%S")
                obstime = obstime.replace(tzinfo=UTC_tzinfo()).astimezone(Pacific_tzinfo())
                obsdate = obstime.date()
                #midt = obstime.strftime("%Y/%m/%d %H:%M:%S")
                # set to beggining of the day, then back to UTC
                #obstime = obstime.replace(hour=0, minute=1, second=0, microsecond=0, tzinfo=Pacific_tzinfo())
                #midt2 = obstime.strftime("%Y/%m/%d %H:%M:%S")
                #obstime = obstime.astimezone(UTC_tzinfo())
                #endt2 = obstime.strftime("%Y/%m/%d %H:%M:%S")
                #obstimestr = obstime.strftime("DATETIME(%Y, %m, %d, %H, %M, %S)")
                #logging.error("times: %s %s %s %s", startt, midt, midt2, endt2)

                statget = {
                    "TruckStop": lambda: TruckStats.all().filter('day =', obsdate) #EDIT
                    }
                
                survey_d = statget[appname]().get()
                #survey_q = db.GqlQuery("SELECT * FROM WorkStats WHERE day = :1", obstimestr)
                #survey_d = survey_q.get()
                if survey_d == None:
                    pickstats = {
                        "TruckStop": lambda: TruckStats() #EDIT
                        }

                    survey_d = pickstats[appname]()
                    survey_d.total = 1
                    survey_d.day = obstime.date()
                    #logging.error("Picked a new one %s" % (obstimestr))
                else:
                    survey_d.total = survey_d.total + 1
                survey_d.put()

                stattotalget = {
                    "TruckStop": lambda: TruckStatsTotal.all()  #EDIT
                    }
                statstotal = stattotalget[appname]().get()
                if statstotal == None:
                    pickstats = {
                        "TruckStop": lambda: TruckStatsTotal()  #EDIT
                        }
                    statstotal = pickstats[appname]()
                    statstotal.total = 1
                else:
                    statstotal.total = statstotal.total + 1
                statstotal.put()
                    

                # write into CSV Blob


                
            except db.Error, err:
                logging.error('error inserting to database' + str(type(err)) + ' ' + str(err.args) + ' ' + str(err))
                self.error(401)
                return
        else:
            logging.error('request token empty')
            self.error(401)


class deleteit(webapp.RequestHandler):
    def post(self):
        self.handle()
    def get(self):
        self.handle()

    def handle(self):
        q = db.GqlQuery("SELECT __key__ FROM PathSurveyTrace")

        results = q.fetch(500)
        
        db.delete(results)





        
#####################################
# config & main
#####################################

application = webapp.WSGIApplication(
                                     [('/', HomePage),
                                      ('/map', MapPage),
                                      ('/clients', ClientsPage),
                                      ('/about', AboutPage),
                                      ('/get_app_data', GetAppData),
                                      ('/dashboard', Dashboard),
                                      ('/dashboardData', DashboardData),
                                      ('/dashboardPathTrace', DashboardPathTrace),
                                      ('/getimageData', GetImageData),
                                      ('/request_token', RequestTokenHandler),
                                      ('/authorize', UserAuthorize),
                                      ('/access_token', AccessTokenHandler),
                                      ('/authorize_access', AuthorizeAccessHandler),
                                      ('/protected_upload', ProtectedResourceHandler),
                                      ('/protected_upload2', ProtectedResourceHandlerUploadAll),
                                      ('/protected_upload_home', ProtectedResourceHandlerUploadAll),
                                      ('/protected_upload_path', ProtectedResourceHandlerUploadAll),
                                      ('/protected_upload_pathtrace', ProtectedResourceHandlerBHPathTrace),
                                      ('/protected_upload_work', ProtectedResourceHandlerUploadAll),
                                      ('/protected_upload_after', ProtectedResourceHandlerUploadAll),
                                      ('/protected_upload_school', ProtectedResourceHandlerUploadAll),
                                      ('/deleteit', deleteit),
                                      ('/create_consumer', CreateConsumer),
                                      ('/get_consumer', GetConsumer),
                                      ('/create_user', CreateUser),
                                      ('/confirm_user', ConfirmUser)],
                                     debug=True)



def main():
    run_wsgi_app(application)

if __name__ == "__main__":
    main()



