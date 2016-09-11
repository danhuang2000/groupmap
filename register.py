# Copyright 2016 Google Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import webapp2
import os
import json
import cloudstorage as gcs


class RegistrationPage(webapp2.RequestHandler):
   def register(self, newuser):
      retval = None
      names = {}
      filename = "/lession1-182018.appspot.com/names.txt"
      try:
          f = gcs.open(filename, "r")
          names = json.load(f)
          if newuser in names.keys():
             retval = "Name already exists"
          f.close()
      except gcs.NotFoundError:
          names = {} 

      if newuser != None:
         names[newuser] = {}
         f = gcs.open(filename, "w")
         json.dump(names, f)
         f.close()

      return retval

   def get(self):
      username = self.request.get("username")
      msg = self.register(username)
      self.response.headers['Content-Type'] = 'text/plain'
      self.response.write(msg)

app = webapp2.WSGIApplication([
    ('/register', RegistrationPage),
], debug=True)
