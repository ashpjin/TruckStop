

from time import time

from google.appengine.ext import db
from google.appengine.api import urlfetch




# class HomeSurvey(db.Model):
#     username =      db.StringProperty()
#     timestamp =     db.DateTimeProperty(auto_now_add=True) # submitted time
#     longitude =     db.StringProperty()
#     latitude =      db.StringProperty()
#     time =          db.IntegerProperty()                   # observation time
#     # time should be a dbDateTimePropertly(), but the android app
#     # is currently doing a getTime() which returns milliseconds since 1970,
#     # and python does not have an easy way to parse that with strptime()
#     #time =          db.DateTimeProperty()
#     version =       db.StringProperty()
#     imei =          db.StringProperty()
#     q0_r0 =           db.StringProperty(multiline=True) # when did you get home
#     q1_r0 =           db.StringProperty()               # why are you going out
#     q1_r1 =           db.StringProperty()               # where are you going out
#     q2_r0 =           db.StringProperty()               # Where was food prepared
#     q2_picture =      db.BlobProperty()                 # picture of food
#     #q2_picture =      db.ReferenceProperty(SurveyPhoto) # picture of food
#     q3_r0 =           db.StringProperty()               # concern level
#     q3_r1 =           db.StringProperty()               # concern label
#     q3_picture =      db.BlobProperty()                 # picture of repair item
#     #q3_picture =      db.ReferenceProperty(SurveyPhoto) # picture of repair item
#     q4_r0 =           db.StringProperty()               # concern level
#     q4_r1 =           db.StringProperty()               # concern label
#     q4_picture =      db.BlobProperty()                 # picture of repair item
#     #q4_picture =      db.ReferenceProperty(SurveyPhoto) # picture of repair item
#     q5_r0 =           db.StringProperty()               # concern level
#     q5_r1 =           db.StringProperty()               # concern level
#     q5_picture =      db.BlobProperty()                 # picture of repair item
#     #q5_picture =      db.ReferenceProperty(SurveyPhoto) # picture of repair item
#     q6_r0 =           db.StringProperty()               # concern level
#     q6_r1 =           db.StringProperty()               # concern level
#     q6_picture =      db.BlobProperty()                 # picture of home uncomfort
#     #q6_picture =      db.ReferenceProperty(SurveyPhoto) # picture of home uncomfort
#     q7_r0 =           db.StringProperty(multiline=True) # freetext concern
#     #survey_ref =      db.ReferenceProperty(Survey)      # link back to servey


class TruckSurvey(db.Model):
    username =      db.StringProperty()
    timestamp =     db.DateTimeProperty(auto_now_add=True) # submitted time
    longitude =     db.StringProperty()
    latitude =      db.StringProperty()
    time =          db.IntegerProperty()                   # observation time
    timefull =      db.DateTimeProperty()                   # observation time
    version =       db.StringProperty()
    imei =          db.StringProperty()
    r_0 =           db.StringProperty(multiline=True)
    r_1 =           db.StringProperty(multiline=True)
    r_2 =           db.StringProperty(multiline=True)
    r_3 =           db.StringProperty(multiline=True)
    r_4 =           db.StringProperty(multiline=True)
    i_0 =           db.BooleanProperty()

class TruckImages(db.Model):
    image =         db.BlobProperty()
    image_index =   db.StringProperty()
    survey_ref =    db.ReferenceProperty(TruckSurvey)

class TruckStats(db.Model):
    day = db.DateProperty()
    total = db.IntegerProperty()

class TruckStatsTotal(db.Model):
    total = db.IntegerProperty()



