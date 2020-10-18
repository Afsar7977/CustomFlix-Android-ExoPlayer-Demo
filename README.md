# CustomFlix-Android-ExoPlayer-Demo
A video-player based application which used ExoPlayer core built-in functionalities for playing any complex video smoothly. 

<img src="https://github.com/Afsar7977/CustomFlix-Android-ExoPlayer-Demo/blob/master/Photos/homepage.jpg" align="left" width="200" height="400">
<img src="https://github.com/Afsar7977/CustomFlix-Android-ExoPlayer-Demo/blob/master/Photos/player.jpg" align="right" width="200" height="400">

ExoPlayer is an application level media player for Android. It provides an alternative to Android’s MediaPlayer API for playing audio
and video both locally and over the Internet. ExoPlayer supports features not currently supported by Android’s MediaPlayer API, 
including DASH and SmoothStreaming adaptive playbacks. Unlike the MediaPlayer API, ExoPlayer is easy to customize and extend, 
and can be updated through Play Store application updates.

In ExoPlayer every piece of media is represented by a MediaItem. However internally, the player needs MediaSource instances to play the content.
The player creates these from media items using a MediaSourceFactory.

By default the player uses a DefaultMediaSourceFactory, which can create instances of the following content MediaSource implementations:
- DashMediaSource for DASH.
- SsMediaSource for SmoothStreaming.
- HlsMediaSource for HLS.
- ProgressiveMediaSource for regular media files.
DefaultMediaSourceFactory can also create more complex media sources depending on the properties of the corresponding media items. 

ExoPlayer provides functionality to download media for offline playback. In most use cases it’s desirable for downloads to continue
even when your app is in the background. For these use cases your app should subclass DownloadService, and send commands to the service to add,
remove and control the downloads. The diagram below shows the main classes that are involved.
