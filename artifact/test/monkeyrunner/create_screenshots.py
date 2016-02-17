# Parameters

# Paths are relative to the path of this script
PATH_APK = '../../../app/build/outputs/apk/app-debug.apk'
PATH_IMAGE = '../../../artifact/screenshots/'
PATH_IMAGE_REFERENCE = '../../../artifact/screenshots/v0.21/'

DELAY = 5

# Global variables

pathPrefix = ''
pathImage = ''
pathImageReference = ''
screenshotNumber = 0
screenshotErrors = 0
device = 0

from com.android.monkeyrunner import MonkeyRunner, MonkeyDevice
import os

def main():
	setup()
	runScenario()
	summary()

def setup():
	global pathPrefix, pathImage, pathImageReference
	
	pathPrefix = os.path.dirname(os.path.abspath(__file__)) + '\\' # path to current script
	pathImage = pathPrefix + PATH_IMAGE
	pathImageReference = pathPrefix + PATH_IMAGE_REFERENCE

def summary():
	print '%d of %d images FAILED comparison' % (screenshotErrors, screenshotNumber)

def screenshot(title):
	global screenshotNumber, screenshotErrors
	
	screenshotNumber += 1
	filename = '%02d-%s.png' % (screenshotNumber, title)
	fullpath = '%s%s' % (pathImage, filename)
	#print "   Saving screen shot to file %s" % filename
	
	screenshot = device.takeSnapshot()
	screenshot.writeToFile(fullpath, 'png')
	
	fullpath2 = '%s%s' % (pathImageReference, filename)
	reference = MonkeyRunner.loadImageFromFile(fullpath2)

	try:
		if not screenshot.sameAs(reference, 0.9):
			print '   ERROR: Comparison with reference failed! File: %s' % filename
			screenshotErrors += 1
	except:
		print '   WARNING: Reference image not found.'
		screenshotErrors += 1
	
def runScenario():
	global device
	
	print "Connecting to device..."
	device = MonkeyRunner.waitForConnection()

	print "Installing package..."
	pathApk = pathPrefix + PATH_APK
	device.installPackage(pathApk)

	print "Starting activity Calendar..."
	package = 'cz.jaro.alarmmorning'
	activity = 'cz.jaro.alarmmorning.AlarmMorningActivity'
	runComponent = package + '/' + activity
	device.startActivity(component=runComponent)

	MonkeyRunner.sleep(DELAY)

	screenshot("calendar")

	print "Showing menu..."
	device.touch(80, 150,  MonkeyDevice.DOWN_AND_UP)

	MonkeyRunner.sleep(DELAY)
	
	screenshot("menu")

	print "Starting activity Default..."
	device.touch(80, 430,  MonkeyDevice.DOWN_AND_UP)

	MonkeyRunner.sleep(DELAY)
	
	screenshot("defaults")

	print "Going back..."
	device.touch(80, 150,  MonkeyDevice.DOWN_AND_UP)
	#device.press("KEYCODE_BACK", MonkeyDevice.DOWN_AND_UP)

	MonkeyRunner.sleep(DELAY)

	print "Showing menu..."
	device.touch(80, 150,  MonkeyDevice.DOWN_AND_UP)

	MonkeyRunner.sleep(DELAY)
	
	print "Starting activity Settings..."
	device.touch(80, 580,  MonkeyDevice.DOWN_AND_UP)

	MonkeyRunner.sleep(DELAY)
	
	screenshot("settings")

	print "Going back..."
	device.touch(80, 150,  MonkeyDevice.DOWN_AND_UP)
	#device.press("KEYCODE_BACK", MonkeyDevice.DOWN_AND_UP)

	MonkeyRunner.sleep(DELAY)

	print "Starting activity Ring..."
	package = 'cz.jaro.alarmmorning'
	activity = 'cz.jaro.alarmmorning.RingActivity'
	runComponent = package + '/' + activity
	device.startActivity(component=runComponent)

	MonkeyRunner.sleep(DELAY)

	screenshot("ring")
	
	print "Dismiss..."
	device.touch(540, 1700,  MonkeyDevice.DOWN_AND_UP)

if __name__ == '__main__':
     main()
