
# Cordova EncPlayer Plugin

Cordova media player plugin using Google's ExoPlayer framework.

## Sample project creation by using this plugin

Create a new Cordova Project

    $ cordova create hello
    
Install the plugin

    $ cd hello
    $ cordova plugin add https://github.com/Lambo008/encplayer_plugin.git
    

Please create event.js in the www/js folder

```js
    (function(window){
    
       $('#start').click(function(){
          "use strict";
           console.log(navigator);

           var params = {
               url: "encplayer"
           };

           window.ExoPlayer.startPlayer(params, captureSuccess, captureError);
    
       });
    
        // capture callback
       function captureSuccess(result) {
          "use strict";
           let str = result; //JSON.stringify(result);
           console.log(str);			   
           $("#result_str").text("result:"+str);
       };
   
       // capture error callback
       var captureError = function(error) {
           console.error("Error code: ", error.code);
           $("#result_str").text("OCR failed:"+error.message);
       };
   
   })(window);
```

Please add this code to the index.html for button, and include these codes.
```
<button class="btn btn-block btn-primary" id="start">Play</button>
...

<script src="lib/jquery-3.2.1.js"></script>
<script type="text/javascript" src="js/event.js"></script>

```
Please make the lib folder in www directory, and copy the jquery-3.2.1.js.

If we click the start button, we call the startplayer function of the www/exoplayer.js.
At that time, exoplayer calls the EncPlayerPlugin.java file for interfacing with Cordova and Native java code.
Finally, MainActivity will be opened.
