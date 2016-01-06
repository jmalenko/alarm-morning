from com.android.monkeyrunner import MonkeyRunner, MonkeyDevice, MonkeyImage

PREFIX = 'C:/Users/ext93831/AndroidStudioProjects/alarm-morning/'


DELAY = 4

def main():
	print "Connecting to device..."
	device = MonkeyRunner.waitForConnection()

	print "Installing package..."
	device.installPackage(PREFIX + 'app/build/outputs/apk/app-debug.apk')

	print "Starting activity Calendar..."
	package = 'cz.jaro.alarmmorning'
	activity = 'cz.jaro.alarmmorning.CalendarActivity'
	runComponent = package + '/' + activity
	device.startActivity(component=runComponent)

	MonkeyRunner.sleep(DELAY)

	screenshot = device.takeSnapshot()
	screenshot.writeToFile(PREFIX + 'artifact/shot1.png', 'png')

	reference = MonkeyRunner.loadImageFromFile(PREFIX + 'artifact/shot1-reference.png')
	if not screenshot.sameAs(reference, 0.9):
		print 'ERROR: Comparison failed!'

	print "Showing menu..."
	device.touch(80, 150,  MonkeyDevice.DOWN_AND_UP)

	MonkeyRunner.sleep(DELAY)
	
	screenshot = device.takeSnapshot()
	screenshot.writeToFile(PREFIX + 'artifact/shot2.png', 'png')

	print "Starting activity Default..."
	device.touch(80, 430,  MonkeyDevice.DOWN_AND_UP)

	MonkeyRunner.sleep(DELAY)
	
	screenshot = device.takeSnapshot()
	screenshot.writeToFile(PREFIX + 'artifact/shot3.png', 'png')
	
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
	
	screenshot = device.takeSnapshot()
	screenshot.writeToFile(PREFIX + 'artifact/shot4.png', 'png')
	
	print "Going back..."
	device.press("KEYCODE_BACK", MonkeyDevice.DOWN_AND_UP)

	MonkeyRunner.sleep(DELAY)

	print "Starting activity Ring..."
	package = 'cz.jaro.alarmmorning'
	activity = 'cz.jaro.alarmmorning.RingActivity'
	runComponent = package + '/' + activity
	device.startActivity(component=runComponent)

	MonkeyRunner.sleep(DELAY)

	screenshot = device.takeSnapshot()
	screenshot.writeToFile(PREFIX + 'artifact/shot5.png', 'png')
	
	print "Going to home..."
	device.touch(540, 1700,  MonkeyDevice.DOWN_AND_UP)

	MonkeyRunner.sleep(DELAY)

	device.press("KEYCODE_HOME", MonkeyDevice.DOWN_AND_UP)

if __name__ == '__main__':
     main()