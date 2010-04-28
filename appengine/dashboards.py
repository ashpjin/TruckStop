
import cgi
import os
from django.utils import simplejson as json

import oauth
import hashlib
from datastore import *

from time import time

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

import datetime as datetime_module

from surveymodels import *



# observations per page
PAGE_SIZE = 20

####################
# Utils
####################


# from python tzinfo docs
ZERO = datetime_module.timedelta(0)
class UTC_tzinfo(datetime_module.tzinfo):
    """UTC"""

    def utcoffset(self, dt):
        return ZERO

    def tzname(self, dt):
        return "UTC"

    def dst(self, dt):
        return ZERO



# from the appengine docs
class Pacific_tzinfo(datetime_module.tzinfo):
    """Implementation of the Pacific timezone."""
    def utcoffset(self, dt):
        return datetime_module.timedelta(hours=-8) + self.dst(dt)

    def _FirstSunday(self, dt):
        """First Sunday on or after dt."""
        return dt + datetime_module.timedelta(days=(6-dt.weekday()))

    def dst(self, dt):
        # 2 am on the second Sunday in March
        dst_start = self._FirstSunday(datetime_module.datetime(dt.year, 3, 8, 2))
        # 1 am on the first Sunday in November
        dst_end = self._FirstSunday(datetime_module.datetime(dt.year, 11, 1, 1))

        if dst_start <= dt.replace(tzinfo=None) < dst_end:
            return datetime_module.timedelta(hours=1)
        else:
            return datetime_module.timedelta(hours=0)
    def tzname(self, dt):
        if self.dst(dt) == datetime_module.timedelta(hours=0):
            return "PST"
        else:
            return "PDT"


# hander for: /
class HomePage(webapp.RequestHandler):
    def get(self):
        path = os.path.join (os.path.dirname(__file__), 'views/dashboard.html')
        self.response.out.write (template.render(path, {}))
        
#handler for /clients
class ClientsPage(webapp.RequestHandler):
    def get(self):
        path = os.path.join (os.path.dirname(__file__), 'views/clients.html')
        self.response.out.write (template.render(path, {}))
        
#handler for /about
class AboutPage(webapp.RequestHandler):
    def get(self):
        path = os.path.join (os.path.dirname(__file__), 'views/about.html')
        self.response.out.write (template.render(path, {}))
        

class GetImageData(webapp.RequestHandler):
    # This is so that we can display images on the map
    # and provide links to images in the data tables.
    def get(self):
        # We directly return the jpeg data
        self.response.headers['Content-type'] = 'image/jpeg'
        req_key = self.request.get('key')
        pic = self.request.get('i')
        appname = self.request.get('appname')
        if req_key != '' and appname != '' and pic != '':
            try :
                db_key = db.Key(req_key)

                pickimage = {
                    "BHCasa": "SELECT * FROM HomeImages WHERE survey_ref = :1",
                    "BHRuta": "SELECT * FROM PathImages WHERE survey_ref = :1",
                    "BHEscuela": "SELECT * FROM SchoolImages WHERE survey_ref = :1",
                    "BHTrabajo": "SELECT * FROM WorkImages WHERE survey_ref = :1",
                    "BHVecindario": "SELECT * FROM AfterImages WHERE survey_ref = :1"
                    }
                
                db_name = ""
                try:
                    db_name = pickimage[appname]
                except KeyError:
                    logging.error('Could not find app %s' % (appname))
                    self.error(401)
                    return
                
                pi = db.GqlQuery(db_name, db_key).fetch(20)
                for res in pi:
                    if res.image_index == pic:
                        self.response.out.write(res.image)
                        return
            except (db.Error):
                self.error(401)
                return
        self.error(401)
        return


# handler for: /dashboard
class Dashboard(webapp.RequestHandler):
    # A mock up dashboard page
    def get(self):


        if os.environ.get('HTTP_HOST'):
            base_url = 'http://' + os.environ['HTTP_HOST'] + '/'
        else:
            base_url = 'http://' + os.environ['SERVER_NAME'] + '/'


        template_values = { 'base_url' : base_url }

        av = {}
        #days = []
        startday = datetime_module.datetime.now().replace(tzinfo=UTC_tzinfo()).astimezone(Pacific_tzinfo()).date()
        # endday is two weeks ago
        endday = startday - datetime_module.timedelta(days=14)
        
        for i in range(15):
            thedt = startday - datetime_module.timedelta(days=i)
            ds = thedt.strftime("%Y-%m-%d")
            ds = thedt.strftime("%Y-%m-%d")
            daysort = int(thedt.strftime("%Y%m%d"))
            av[ds] = {
                'day': ds,
                'daysort' : daysort,
                'work': 0,
                'home': 0,
                'school' : 0,
                'after' : 0,
                'path' : 0
                }
            #days.append(ds)
  
    
        def mycmp(x, y):
            return y['daysort'] - x['daysort']
        
        # logging.error("%s %s %s" % (startday.strftime("%Y-%m-%d"), endday.strftime("%Y-%m-%d"), allwork))
        
        #template_values['days'] = days
        
        avv = av.values()
        avv.sort(cmp=mycmp)
        template_values['stats'] = avv
        
        # logging.error("%s" % (template_values))
        path = os.path.join (os.path.dirname(__file__), 'views/dashboard.html')
        self.response.out.write (template.render(path, template_values))

#         subday = datetime_module.timedelta(days=1)
#         now = datetime_module.datetime.now() + subday
#         now = now.replace(hour=0, minute=0, second=0, microsecond=0, tzinfo=Pacific_tzinfo()).astimezone(UTC_tzinfo())
#         yest = datetime_module.datetime.now()
#         yest = yest.replace(hour=0, minute=0, second=0, microsecond=0, tzinfo=Pacific_tzinfo()).astimezone(UTC_tzinfo())
#         yest = yest
        
        
#         for shift in range(8):
        
        
#             nowstr = now.strftime("DATETIME(%Y, %m, %d, %H, 0, 0)")
#             yeststr = yest.strftime("DATETIME(%Y, %m, %d, %H, 0, 0)")
#             survey_data = db.GqlQuery("SELECT * from PathSurvey WHERE timefull > :1 and timefull < :2", yeststr, nowstr).fetch(1000)
#             template_values['ruta_day' + str(shift)] = 0
#             c = 0
#             for s in survey_data:
#                 c = c + 1
#             template_values['ruta_day' + str(shift)] = c                
#             template_values['date_day' + str(shift)] = now.strftime("%Y/%m/%d")
#             logging.error("%s to %s - %s" % (yeststr, nowstr, str(template_values['ruta_day' + str(shift)])))
#             now = now - subday
#             yest = yest - subday
        



class DashboardData(webapp.RequestHandler):
    # A mock up dashboard page
    def get(self):

        if not users.is_current_user_admin():
            self.response.out.write(
                "Admin Only Access... you need to <a href=" +
                users.create_login_url("http://bh-survey.appspot.com/dashboard") +
                ">login</a>")
            return
        
        if os.environ.get('HTTP_HOST'):
            base_url = 'http://' + os.environ['HTTP_HOST'] + '/'
        else:
            base_url = 'http://' + os.environ['SERVER_NAME'] + '/'

        template_values = { 'base_url' : base_url }

        forward = True

        page = 1

        if self.request.get('page'):
            page = int(self.request.get('page'))

            
        if page == 1:
            template_values['prevpage'] = None
        else:
            template_values['prevpage'] = page - 1

        template_values['nextpage'] = page + 1
        

        appname = self.request.get('appname')
        
        picksurvey = {
            "TruckStop": lambda: TruckSurvey.all().order('-timefull')  #EDIT
            }

        survey_query = None
        
        try:
            survey_query = picksurvey[appname]()
        except KeyError:
            logging.error("Can't find appname data: %s" % (appname))
            self.error(401)
            return

        template_values['appname'] = appname

	survey_data = survey_query.fetch(PAGE_SIZE, offset=PAGE_SIZE*(page-1))

        for s in survey_data:
            s.timestamp = s.timestamp.replace(tzinfo=UTC_tzinfo()).astimezone(Pacific_tzinfo())
            if s.timefull is not None:
                s.timefull = s.timefull.replace(tzinfo=UTC_tzinfo()).astimezone(Pacific_tzinfo())
            s.time = int(datetime_module.datetime.fromtimestamp(s.time/1000.0).strftime("%Y%m%d%H%M%S"))

            #s.timefull = s.timefull.astimezone(Pacific_tzinfo())

        template_values['survey'] = survey_data

        picktemplate = {
            "TruckStop": 'views/dashboard.html' #EDIT
            }
        
        path = os.path.join (os.path.dirname(__file__), picktemplate[appname])
        self.response.out.write (template.render(path, template_values))



class DashboardPathTrace(webapp.RequestHandler):
    # A mock up dashboard page
    def get(self):

        if users.is_current_user_admin():
            u = self.request.get('user')
            if u != '':
                sv = db.GqlQuery("SELECT * FROM PathSurveyTrace WHERE username = :1 ORDER BY time DESC LIMIT 250", u)

                news = []
                for s in sv:
                    s.timestamp = s.timestamp.replace(tzinfo=UTC_tzinfo()).astimezone(Pacific_tzinfo())
                    if s.timefull is not None:
                        s.timefull = s.timefull.replace(tzinfo=UTC_tzinfo()).astimezone(Pacific_tzinfo())
                    s.time = int(datetime_module.datetime.fromtimestamp(s.time/1000.0).strftime("%Y%m%d%H%M%S"))
                    news.append(s)

                template_values = {
                    'pathsurveytrace' : news,
                    'username' : u
                    }
                
                
                path = os.path.join (os.path.dirname(__file__), 'views/dashboard_pathtrace.html')
                self.response.out.write (template.render(path, template_values))
        else:
            
            self.response.out.write(
                "Admin Only Access... you need to <a href=" +
                users.create_login_url("http://bh-survey.appspot.com/dashboardPathTrace?user=martin9") +
                ">login</a>")
            #self.response.out.write ("Admin Only Access<br>%s not allowed in!" % (users.get_current_user().nickname()))


# handles requests to BASE_URL/map?appname=APP_NAME
# renders map for app specified by APP_NAME argument
class MapPage(webapp.RequestHandler):
    def get(self):
        appname = self.request.get('appname')
        pickmap = {
            "BHCasa": 'views/map_home.html',
            "BHRuta": 'views/map_path.html',
            "BHEscuela": 'views/map_school.html',
            "BHTrabajo": 'views/map_work.html',
            "BHVecindario": 'views/map_after.html'
            }
        path = os.path.join (os.path.dirname(__file__), pickmap[appname])
        self.response.out.write (template.render(path, {}));


# handles requests to BASE_URL/get_app_data?appname=APP_NAME&startDaysBefore=S&endDaysBefore=E
# returns relevant data to APP_NAME in JSON format
# returns data from startDaysBefore days before now to endDaysBefore days before now.
# ex. startDaysBefore=14&endDaysBefore=0 returns data from past 2 weeks 
class GetAppData(webapp.RequestHandler):
    def get(self):
        if not users.is_current_user_admin():
            #self.response.out.write(
                # "Admin Only Access... you need to <a href=" +
                # users.create_login_url("http://bh-survey.appspot.com/dashboard") +
                # ">login</a>")
            self.response.out.write("No Access to Data")
            return

        appname = self.request.get('appname')
        startDaysBefore = self.request.get('startDaysBefore')
        endDaysBefore = self.request.get('endDaysBefore')     

        if startDaysBefore == "" or endDaysBefore == "":
            surveyQuery = {
            "BHCasa": lambda: HomeSurvey.all().order('-timefull'),
            "BHRuta": lambda: PathSurvey.all().order('-timefull'),
            "BHEscuela": lambda: SchoolSurvey.all().order('-timefull'),
            "BHTrabajo": lambda: WorkSurvey.all().order('-timefull'),
            "BHVecindario": lambda: AfterSurvey.all().order('-timefull'),
            }

        else:
            startDaysBeforeInt = int(startDaysBefore)
            endDaysBeforeInt = int(endDaysBefore)
            timeNow = datetime_module.datetime.now().replace(tzinfo=UTC_tzinfo()).astimezone(Pacific_tzinfo()).date()
            startDay = timeNow - datetime_module.timedelta(days=startDaysBeforeInt)
            endDay = timeNow - datetime_module.timedelta(days=endDaysBeforeInt)

            surveyQuery = {
                "BHCasa": lambda: HomeSurvey.all().filter("timestamp >= ", startDay).filter("timestamp <= ", endDay).fetch(1000),
                "BHRuta": lambda: PathSurvey.all().filter("timestamp >= ", startDay).filter("timestamp <= ", endDay).fetch(1000),
                "BHEscuela": lambda: SchoolSurvey.all().filter("timestamp >= ", startDay).filter("timestamp <= ", endDay).fetch(1000),
                "BHTrabajo": lambda: WorkSurvey.all().filter("timestamp >= ", startDay).filter("timestamp <= ", endDay).fetch(1000),
                "BHVecindario": lambda: AfterSurvey.all().filter("timestamp >= ", startDay).filter("timestamp <= ", endDay).fetch(1000)
                }

        surveyData = None
        
        try:
            surveyData = surveyQuery[appname]()
        except KeyError:
            logging.error("Can't find appname data: %s" % (appname))
            self.error(401)
            return

        d = []
        i = 0
        for s in surveyData:
            e = {}
            s.timestamp = s.timestamp.replace(tzinfo=UTC_tzinfo()).astimezone(Pacific_tzinfo())
            if s.timefull is not None:
                s.timefull = s.timefull.replace(tzinfo=UTC_tzinfo()).astimezone(Pacific_tzinfo())
            s.time = int(datetime_module.datetime.fromtimestamp(s.time/1000.0).strftime("%Y%m%d%H%M%S"))
            e['time'] = s.timestamp.strftime("%Y-%m-%d, %H:%M:%S")
            e['username'] = s.username            
            e['latitude'] = s.latitude
            e['longitude'] = s.longitude
            
            d.append(e)
            i = i + 1
        
        if i > 0:
            self.response.out.write(json.dumps(d))
        else:
            self.response.out.write("No Data Found")

