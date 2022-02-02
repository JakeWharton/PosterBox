# Poster Box

Rotating display of Movie and TV posters.

## Configuration

The configuration file allows customization of behavior and specifies the source of content.
All top-level keys and sections are optional.
An empty configuration file is technically valid, but will result in no content being displayed.

```toml
# Item display duration in seconds (optional).
#  Value: must be positive integer
#  Default: 15
itemDisplayDuration = "15"

# Transition between items (optional).
#  Value: "none", "fade", "crossfade", "slide-right", or "slide-left"
#  Default: "fade"
itemTransition = "slide-right"

# Use Plex as a content source.
#  This section is optional but has required keys if it is present.
[plex]
# Plex host URL (required).
#  Value: DNS hostname or IP with protocol and optional port.
host = "http://plexms:32800"
# Plex authentication token (required).
token = "abc123"
# Libraries from which to pull posters (optional). No matter the contents of this array,
# only entries of type "movie" or "show" will be used.
#  Value: Array of case-sensitive library names or null to use all available libraries.
#  Default: null
libraries = [ "Movies", "TV Shows" ]
# Minimum rating cut-off for poster inclusion (optional). Content with no rating is
# treated as having a rating of 0. Audience rating takes precedence over critic rating.
#  Value: Whole number in the range [0, 100]
#  Default: 0
minimumRating = 40
```


## License

    Copyright 2022 Jake Wharton

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
