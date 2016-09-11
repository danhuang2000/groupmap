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


class UpdatePage(webapp2.RequestHandler):
   def update(self, username, x, y):
      retval = None
      names = {}
      filename = "/lession1-182018.appspot.com/names.txt"
      try:
          f = gcs.open(filename, "r")
          names = json.load(f)
          f.close()
          if username in names.keys():
             names[username]["x"] = x
             names[username]["y"] = y
             f = gcs.open(filename, "w")
             json.dump(names, f)
             f.close()
      except gcs.NotFoundError:
          retval = "Not found"

      return retval

   def get(self):
      username = self.request.get("username")
      x = self.request.get("x")
      y = self.request.get("y")
      msg = self.update(username, x, y)
      self.response.headers['Content-Type'] = 'text/plain'
      self.response.write(msg)

app = webapp2.WSGIApplication([
    ('/update', UpdatePage),
], debug=True)
