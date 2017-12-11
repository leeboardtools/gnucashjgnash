This is a limited converter for converting a fairly simple Gnucash database into an equivalent jGnash database.
This has only been tested on GnuCash V 2.6.17/2.6.18

Some of items to note (most of this is based upon information in the Gnucash document
https://github.com/Gnucash/gnucash/blob/master/libgnucash/doc/xml/gnucash-v2.rnc):

- Accounts:
    The following account types are not supported:
        - Receivable
        - Payable
        - Trading
        - Creditline

- Stocks:
    - Stocks are imported based upon the stock symbol, such as AAPL (cmdty:id in the XML file), the Type (NASDAQ, NYSE, etc.)
    is currently ignored.
    - Only quote sources supported are yahoo, yahoo_australia, and yahoo europe (mapped to yahoo_uk).
    - Quote time zones are ignored.

- Stock pricing:
    - The commodity space (price:commodity/cmdty:space) is ignored.
    - The currency (price:currency) is ignored.
    - The price type (price:type) is ignored.
    - The time zone (the offset in price:time) is ignored.
    - There is no high/low/volume, high and low are set to the current price, volume is set to 0.
    - The source (price:source) is ignored.

    
    
    
Project Development Notes:
Setting up the environment:
	- Install JDK 1.8.0_151, either from Oracle (Windows, OSX) or OpenJDK (Linux)
	- Install Eclipse to build GnuCashJGnash
	- Create or choose a folder where everything will go. I'll refer to it as the DEV folder.
	
	- Build jGnash (you only need to build jGnash if you want to be able to debug in it)
	- Clone the jGnash git repository: https://github.com/ccavanaugh/jgnash.git into
		the DEV folder.
		From a terminal window:
			cd <DEV>
			git clone https://github.com/ccavanaugh/jgnash.git
		Within the DEV folder you should now have something like:
			DEV
				+jgnash
					+deployfx
					+gradle
					+jgnash-bayes
					...
		
	- Still in the terminal window, build jGnash:
		-Linux:
			cd jgnash
			./gradlew clean distZip
	
		-Windows (per jGnash Readme.ME, I haven't actually tried this)
			cd jgnash
			gradlew clean distZIP
		
	- When the build is finished you should have a jgnash-2.xx.x-bin.zip file in the jgnash folder.
	- Extract the contents of that .ZIP file to the DEV folder, so you have a folder named jGnash-2.xx.x
		in the DEV folder.
	- Rename the jGnash-2.xx.x project as jGnash_install
	- The DEV folder should now look something like:
		+DEV
			+jgnash
				...
			+jgnash_install
				+bin
				+lib
				+plugins
				...
	
Setting up a new Eclipse project:
	- Why Eclipse?
		- IntelliJ was driving me crazy with the arrow keys on the numpad not working in Ubuntu 16.04
		- I tried Netbeans, the IDE I use most often, but couldn't quite figure out how
			to save the jar file to the jGnash install's plugins folder.
		- Got it to work in Eclipse, so back to Eclipse I go!
	
	- Start with a good old Java Application:
		- From the main menu choose File > New > Java Project
		- Enter the name for your plugin project.
		- Uncheck Use default location, and enter your DEV project, followed by '/' and the folder
			for your plugin.
		- Make sure a 1.8 JRE is chosen for the JRE.
		- Choose Next. The Java Settings page appears.
		- Click the Libraries tab.
		- Choose the Add External JARs button
			- Browse to the DEV/jGnash_install/lib folder
			- Select all the files in the folder and choose OK.
		- Choose the Finish button.
	
	- Make sure your new project is selected in the Package Explorer
	- Choose the File > New > Package menu, and enter a package name for your plugin.
	- Select the newly added package in the Package Explorer, and choose the File > New > Class menu.
	- The New Java Class window appears.
	- Make sure the package is the package you created, then enter a name for your plugin class.
		This is the class that jGnash will look for when it tries to load your plugin.
	- The new class should open up. Replace the contents with the following, leaving the package line at the top.
		XXXXXX

	- Save everything: Choose File > Save All
	- Create a manifest file, jGnash will use this to locate the plugin class:
		- Choose File > New > File
		- Select the top level folder of your project as the parent folder.
		- Enter MANIFEST.MF as the file name.
		- Choose Finish to create the file.
		- IN the file itself, add the following:
Manifest-Version: 1.0
Plugin-Activator: testjgnashplugin.TestJGnashPlugin
Plugin-Version: 2.25

			replacing testjgnashplugin.TestJGnashPlugin with the fully qualified name of your plugin class.

	- Generate the JAR file for your plugin:
		- Choose File > Export, which brings up the Export window.
			- Select Java > JAR file
			- Choose Next
		- The JAR File Specification window appears.
			- Check your project under Select the resources to export. At the least the right half window should
				display .classpath, .project, and MANIFEST.MF, all checked.
			- For Select the export destination, JAR file, choose Browse
				- Browse to the folder DEV/jGnash_install/plugins
				- Enter the name for your plugin JAR file.
				- Choose OK.
			- Choose Next.
		- The JAR Packaging Options window appears.
			- Check Save the description of this JAR in the workspace, it will make things easier later.
			- Choose Browse.
				- Select your project as the parent folder.
				- Enter a name for the file, such as JAR Export.
				- Choose OK.
			- Choose Next.
		- The JAR Manifest Specification window appears.
			- Choose Use existing manifest from workspace.
			- Choose the Browse button after the Manifest file.
				- Select MANIFEST.MF from your project.
				- Choose OK.
			- Choose Finish.

		- Eclipse doesn't say anything about the JAR file being successfully created, so verify it exists in your 
			DEV/jgnash_install/plugins folder.

	- Create a Run configuration so you can launch jGnash from Eclipse:
		- Choose Run > Run Configurations. The Run Configurations window appears.
		- Select Java Application.
		- Press the New button to create a new launch configuration.
			- Enter a name for the configuration, say 'jGnash'.
			- Select the Main tab if it is not selected.
			- Choose the Browse button for Project.
				- Select your project.
				- Choose OK.
			- Choose the Search button for Main class.
				- The Select Main Type window appears.
				- Type jgn in Select type.
				- Matching items should display 'jGnashFx - (default package)'.
				- With 'jGnashFx - (default package)' selected, choose the OK button.
			- Choose the Run button, jGnash should launch.

	- Hopefully jGnash launched successfully!
		- The Eclipse Console window will hopefuly display a line similar to:
			INFO: Starting plugin: yourplugin
		- The ultimate test will be to open the File > Import menu in jGnash, it should show an item
			'Test JGnash Plugin', which was added by the plugin.


		
