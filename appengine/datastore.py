#!/usr/bin/env python
#
# Copyright 2007 Google Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#	 http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

import oauth
import hmac
import hashlib
import binascii
import string
from time import time

from models import *
from urlparse import urlparse


from google.appengine.ext import db
from google.appengine.api import urlfetch

from google.appengine.ext import webapp
from google.appengine.ext.webapp import util

MAX_URL_LENGTH = 2083 # http://www.boutell.com/newfaq/misc/urllength.html
OAUTH_BLACKLISTED_HOSTNAMES = []

# implements oauth's OAuthDataStore methods
class DataStore(oauth.OAuthDataStore):
	def __init__(self):
		self.consumer = None
		self.request_token = None
		self.access_token = None

	# get OAuthConsumer object if consumer exists
	def lookup_consumer(self, key):
		my_consumer = Consumer().get_consumer(key)
		if not my_consumer:
			return None
		self.consumer = my_consumer
		return my_consumer
	# end lookup_consumer

	# get OAuthToken object if token key and type exist
	def lookup_token(self, token_type, token):
		if token_type != 'request' and token_type != 'access':
			raise oauth.OAuthError('Invalid token type: ' + token_type)

		my_token = Token().get_token(token)
		if not my_token:
			return None
		if token_type == 'request':
			self.request_token = my_token
		elif token_type == 'access':
			self.access_token = my_token
		return my_token
	# end lookup_token

	# get nonce key if consumer key, token key, and nonce key exist
	# else, save new nonce?
	def lookup_nonce(self, oauth_consumer, oauth_token, nonce):
		if not oauth_consumer:
			return None
		if not oauth_token:
			return None
		if not nonce:
			return None

		my_nonce_key = Nonce().get_nonce_key(nonce, oauth_token.key, oauth_consumer.key)

		if not my_nonce_key:
			Nonce().insert_nonce(nonce, oauth_token.key, oauth_consumer.key)
			return None
		return my_nonce_key
	# end lookup_nonce
	
	# 
	def fetch_request_token(self, oauth_consumer, oauth_callback):
		if not self.consumer:
			raise oauth.OAuthError('Consumer not set')
		if oauth_consumer.key != self.consumer.key:
			raise oauth.OAuthError('Consumer key does not match.')

		if not oauth_callback:
			raise oauth.OAuthError('No callback defined')
		if not check_valid_callback(oauth_callback):
			raise oauth.OAuthError('Invalid callback URL.')

		oauthtok = Token().insert_request_token(oauth_consumer.key, oauth_callback)
		if not oauthtok:
			raise oauth.OAuthError('Could not create request token')

		self.request_token = oauthtok
		return oauthtok
	# end fetch_request_token

	def fetch_access_token(self, oauth_consumer, oauth_token, oauth_verifier):
		if not self.consumer or not self.request_token:
			raise oauth.OAuthError('consumer or request token not yet set')

		if oauth_consumer.key != self.consumer.key:
			raise oauth.OAuthError('consumer token does not match')
		if oauth_token.key != self.request_token.key:
			raise oauth.OAuthError('request token does not match')

		# if is_approved true, callback should already be confirmed
		#if not self.request_token.is_approved:
		tok = Token().gql('WHERE ckey = :1 AND is_approved = True', self.request_token.key).get()
		if not tok:
			raise oauth.OAuthError('request token not yet approved')

		# check here if token is authorized
		if oauth_verifier != self.request_token.verifier:
			raise oauth.OAuthError('verifier does not match')

		# this should take consumer key and verifier
		oauthtok = Token().insert_access_token(oauth_consumer.key, oauth_verifier, tok.user)
		if not oauthtok:
			return None

		self.access_token = oauthtok

		return self.access_token
	# end fetch_access_token

	def authorize_request_token(self, oauth_token, user):
		if not self.request_token:
			raise oauth.OAuthError('request token not yet set')

		if oauth_token.key == self.request_token.key:
			# authorize the request token in the store
			oauthtok = Token().approve_token(oauth_token.key, user)

			self.request_token = oauthtok
			return self.request_token

		raise oauth.OAuthError('token key does not match')
	# end authorize_request_token
# End Datastore Class

# from django-oauth-appengine
# http://code.welldev.org/django-oauth/wiki/Home
def check_valid_callback(callback):
	"""
	Checks the size and nature of the callback.
	"""
	callback_url = urlparse(callback)
	return (callback_url.scheme
			and callback_url.hostname not in OAUTH_BLACKLISTED_HOSTNAMES
			and len(callback) < MAX_URL_LENGTH)
