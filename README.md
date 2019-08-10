
# Cordova EncPlayer Plugin

Cordova media player plugin using Google's ExoPlayer framework.

## Using

Create a new Cordova Project

    $ cordova create hello
    
Install the plugin

    $ cd hello
    $ cordova plugin add cordova-plugin-encplayer
    

Please create event.js in the www/js folder

```js
    (function(window){
    
       $('#start').click(function(){
          "use strict";
           console.log(navigator);

           var params = {
               url: "http://www.youtube.com/api/manifest/dash/id/bf5bb2419360daf1/source/youtube?as=fmp4_audio_clear,fmp4_sd_hd_clear&sparams=ip,ipbits,expire,source,id,as&ip=0.0.0.0&ipbits=0&expire=19000000000&signature=51AF5F39AB0CEC3E5497CD9C900EBFEAECCCB5C7.8506521BFC350652163895D4C26DEE124209AA9E&key=ik0"
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

```
If we click the start button, we call the startplayer function of the www/exoplayer.js.
At that time, exoplayer calls the EncPlayerPlugin.java file for interfacing with Cordova and Native java code.
Finally, MainActivity will be opened.
