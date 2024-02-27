### Instructions
Add the JitPack repository to your build file.

```
allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
```

Add the dependency in your app build.gradle

```
dependencies {
	        implementation 'com.github.sahjadansari619:CameraWrapper:1.2.3'
	}
```

Create a content URI for image file using FileProvider. Steps to create and configure a FileProvider can be found at https://developer.android.com/reference/android/support/v4/content/FileProvider .
Create an intent and add the content URI as an extra.

```
Intent intent = new Intent(this, CameraActivity.class);
Uri contentUri = FileProvider.getUriForFile(context, getApplicationContext().getPackageName() + ".fileProvider", imgpath);
intent.putExtra(MediaStore.EXTRA_OUTPUT, contentUri);
startActivityForResult(intent, REQUEST_CODE);
```

The `CameraActivity` does not return a thumbnail in `onActivityResult()`, the image should be accessed using the content uri.


To add the Date/Time on Image

```
myIntent.putExtra(CameraActivity.EXTRA_ADD_TIMESTAMP,true)
```

To add custom text on image

 ```
 myIntent.putExtra(CameraActivity.EXTRA_CUSTOM_TEXT,"Your text is here")
 ```
To add location text on image Longitude, Latitude and Accuracy

 ```
 myIntent.putExtra(CameraActivity.EXTRA_ADD_LOCATION,true)
 ```
 
 


                
