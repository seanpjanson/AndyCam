# AndyCam
is a custom camera project resulting from numerous questions on StackOverflow. 
It attempts to address the issues with preview orientation, stretching of images
in both the preview and the picture creation phase, etc...

Logic:
If you need to produce a picture from the Android camera, you have to tackle
the fact that it is the camera that tells you what it can produce, and you as a
developer have to combine the offerings into the final result.

First, let's say you need a picture of size 1600x1200 pixels. The camera gives
you a list of what is available and you have to decide what to pick. Closest ratio
(4:3) or closest size (1700x1100). The logic can be changed and this demo deals with
his issue in CamMgr.pictSz() method.

Second, you have to select from the list of available preview sizes. Choice is even
more difficult, since you have 2 variables to consider:
  - the picture size ratio resulting from your previous step - picture size selection
  - the available screen real-estate that changes with every orientation change, presence
     of menu/action bars, etc...
So, let's say your app picked 1600x1200 picture, the current screen available is
1184x768 and the list of available preview sizes has still different values. This
conundrum is being resolved (sort-of) in the CamMgr.prvwSz() method.
  
Now, you finally have your preview / picture sizes and you have to decide if the preview
size you chose should be letterboxed or pan&scanned (fancy names for fitting two 
rectangles of different size ratios). Letterbox, or fit-in will fit all your image into
the preview rectangle, leaving background colored stripes on two sides.
Pan&Scan mode will fill the screen, letting the image 'overflow' on sides.

The parameters mentioned here can be pre-set in the UT (utility) class.

The code here has a lot of fluff, since it has to deal with a lot of additional issues:

Device orientation can change, but the camera communicates in landscape mode 
(list of offered preview / picture sizes). So the preview must be pre-rotated and the
resulting picture 'rotated back'.

There are also issues related to 'natural device orientation', where some tablets 
(Nexus7-1Gen, Nexus10,...) have default orientation landscape(wide), where other tablets
(Samsung TAB 7,8, Nexus 7-2Gen, ...) behave like large phones, (default orientation
portrait) with added REVERSE PORTRAIT mode (most of standard phones do not have reverse
portrait mode).

Please let me know what the behavior is on your device if you run into trouble.

UPDATE (Feb 21, 2015):

Having nothing better to do, I decided to add 'touch focusing' capability. The camera
is triggered by touching the screen and the touch point is sent to the camera manager 
(CamMgr). The touch point is then used to set up focusing area and the manager asks
camera to focus, (sending notification 'onFocus()' to the activity). When the focusing
is done - can be really slow, the picture is taken. Nothing too fancy.
