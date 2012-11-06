당번약국
=======

현위치 기반으로 대한약사회 홈페이지의 당번약국 정보를 가져와 지도위에 표시해줍니다. 


## Install
당번약국앱은 Google Play에서 설치하실 수 있습니다.

[![Get it on Google Play](http://www.android.com/images/brand/get_it_on_play_logo_small.png)](http://play.google.com/store/apps/details?id=com.alanjeon.dutypharm)

Build
-----

0. 당번약국을 빌드하기 위해서는 maven이 필요합니다.
1. 다음 명령으로 로컬 저장소에 3rd party 라이브러리를 설치합니다.

        ./mvn-libs-install-to-local
2. 당번약국앱에서는 Google Maps 의 Native library, Google Place API, Bugsense API를 사용합니다.
이 라이브러리를 사용하기 위해서는 아래의 각 URL에서 적절한 API를 발급 받습니다.
    * Google Maps - https://developers.google.com/maps/documentation/android/mapkey
    * Google Place API - https://developers.google.com/places/documentation/
    * Bugsense API - https://www.bugsense.com/
    
    이후 발급받은 KEY를 아래 파일에 명시해줍니다.

        src/com/alanjeon/dutypharm/Constants.java

3. 다음과 같은 당번약국 apk를 빌드합니다.

        mvn package


Developed By
============

 * Alan Jeon - <skyisle@gmail.com>

License
=======

    Copyright 2012 Alan Jeon

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
