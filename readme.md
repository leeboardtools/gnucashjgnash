This is a jGnash (https://sourceforge.net/projects/jgnash/) plugin for converting a *Fairly Simple* Gnucash (http://gnucash.org/) database into an equivalent jGnash database.

This has only been tested on GnuCash V 2.6.17/2.6.18 and jGnash 2.33.3, with the testing primarily consisting
of converting my personal GnuCash database into jGnash and verifying the account balances and scheduled transactions.

By fairly simple I mean it handles the features of GnuCash that I had used, which are basically the personal accounting features.
Stuff I consider fancy such as multiple currencies, or business accounts such as accounts receivable/payable are not supported.
I also did not use money market accounts.

# REMEMBER
Make sure you:
- **Are aware that you are using this at your own risk!**
- Verify the balances in the jGnash accounts against your GnuCash accounts!
- Verify any scheduled transactions!
- Get your GnuCash database up-to-date before conversion! (i.e. make sure any pending scheduled transactions, etc. are run)
- Read the following notes!

## Items To Note
Some of the items to note (most of this is based upon information in the GnuCash document
https://github.com/Gnucash/gnucash/blob/master/libgnucash/doc/xml/gnucash-v2.rnc, and might not have a direct feature in GnuCash):

- The binary jGnash file format is used, as this much faster and produces much smaller files than the database or XML formats. You can
always save the binary file to the desired format after conversion.

- Accounts:
	- Only following GnuCash account types are supported:
	 	- Bank
	 	- Cash
	 	- Credit
	 	- Asset
	 	- Liability
	 	- Stock (converted to jGnash Investment)
	 	- Mutual
	 	- Income
	 	- Expense
	 	- Equity
	 	- Root
	 	- Checking
	 	- Savings (converted to jGnash Checking)
	 	- Moneymrkt (this has not been tested)
	 - Smallest currency units (act:commodity-scu and act:non-standard-scu) are currently ignored.

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

- Transactions:
    - Transaction lots are not supported.
    - The post date is used, not the entered date.
    - If both the credit and debit sides of a transaction entry pair have a memo, the memo for the credit side is the one used.
	
- Investment transactions:
    - Lots are ignored.
    - Reinvested dividends:
	   Reinvested dividends in investments are detected by a transaction where one account is an investment or mutual fund account and
	   the other account is an income account. The transaction is entered as two transactions:
        - A dividend transaction recording the income in the income account and the cash addition to the investment account.
        - A buy transaction recording the purchase of the shares using the investment account.
        
- Scheduled transactions:
    - Only fairly simple scheduled transactions are supported. Only one template transaction is supported 
    (may change to a new scheduled transaction is created for each template transaction)
    - Semi-monthly transactions are converted to two separate transactions.
    - End of month transactions are treated as month transactions. The number of days from the end of the month is lost.
    - Reinvested dividends are not supported (they require 2 transactions, but the jGnash Reminder class only supports 1).
    - The last date is not transferred directly, so it's best to make sure your GnuCash database is up to date before transferring.
    - Reminders are only generated for scheduled transactions that have a template transaction.
    - The 'except on weekends' setting is currently ignored, for the end of month it's always set to the last valid day of
    the month.

- Budgets are not supported.

- Discrepancies between GnuCash and jGnash balances:

	As of jGnash 2.33.3, you may see a small difference between the balances of parent accounts displayed in the jGnash Accounts page and the corresponding balance in GnuCash's Accounts tab. As far as I know this should only occur when one or more child accounts contains Investment accounts, or rather accounts that have a market value. This difference is a round-off error due to the way jGnash parent accounts compute their balance for display in the Accounts page.
	
	In jGnash 2.33.3 the parent account sums the market values of the child Investment accounts without rounding the value to cents. This difference can accumulate into several cents difference between the jGnash balance displayed and the GnuCash balance displayed, particularly if there are many child accounts. If you compare the balances of the individual child accounts, and the actual account balance of the parent account, the values should match those of GnuCash.
	
	Something to keep in mind when trying to verify your balances: Look at the inner-most child account balances in the Accounts page, and then look at the internal balance of any parent account (by internal balance I mean open up the parent account and see what the balance at the top of the page shows).

## Warnings
Detected issues are recorded as warnings, which are displayed at the end of the conversion. The warnings are also written to the file Warnings.TXT in the same folder as the GnuCash database file. If there's an existing Warnings.TXT file it is overwritten.

If any warnings are displayed, be sure to review the warnings!


# Installation
To install the plug-in, simply download the GnuCashJGnash.jar file: https://github.com/leeboardtools/gnucashjgnash/blob/master/dist/GnuCashJGnash.jar
and copy it into the plugins folder of jGnash.

To use the plug-in, launch the JavaFx version of jGnash, then go to the File > Import menu and choose Convert GnuCash to jGnash. If Convert GnuCash to jGnash is not in the Import menu, make sure you are running the JavaFx version of jGnash, not the Swing version.

When you're done with the plug-in, simply delete it from the plugins folder of jGnash.


# Project Development Notes:
You can now load and run/debug the plugin from Netbeans 8.2 (the only version I tried), simply open the GnuCashJGnash folder from Netbeans. You will need to follow the Setting up the environment stuff below for the non-Eclipse portions.

Here are my notes on creating a new plugin for jGnash that can be developed and debugged from Eclipse.

## Setting up the environment:
First you'll need to set up your development environment.
- Install JDK 1.8.0_151, either from Oracle (Windows, OSX) or OpenJDK (Linux)

- Install Eclipse to build GnuCashJGnash. I'm using Eclipse Oxygen.1a Release (4.7.1a)
- Create or choose a folder where everything will go. I'll refer to it as the *DEV folder*.

- Build jGnash (you only need to build jGnash if you want to be able to debug in it)
- Clone the jGnash git repository: https://github.com/ccavanaugh/jgnash.git into the *DEV folder*.

    From a terminal window:
> cd <DEV>
> git clone https://github.com/ccavanaugh/jgnash.git

Within the DEV folder you should now have something like:
    
```
 +DEV
   +jgnash
       +deployfx
       +gradle
       +jgnash-bayes
       ...
```

- Still in the terminal window, build jGnash:
    -Linux:
> cd jgnash
> ./gradlew clean distZip

    -Windows (per jGnash Readme.ME, I haven't actually tried this)
> cd jgnash
> gradlew clean distZIP

- When the build is finished you should have a *jgnash-2.xx.x-bin.zip* file in the jgnash folder (where .xx.x 
is the specific jGnash version that was built).
- Extract the contents of that .ZIP file to the *DEV folder*, so you have a folder named *jGnash-2.xx.x*
in the *DEV folder*.
- Rename the jGnash-2.xx.x project as jgnash_install
- The *DEV folder* should now look something like:
```
+DEV
    +jgnash
        ...
    +jgnash_install
        +bin
        +lib
        +plugins
        ...
```	

# Setting up a new Eclipse project:

- Why Eclipse?
    - IntelliJ was driving me crazy with the arrow keys on the numpad not working in Ubuntu 16.04
    - I tried Netbeans, the IDE I use most often, but couldn't quite figure out how
        to save the jar file to the jGnash install's plugins folder.
    - Got it to work in Eclipse, so back to Eclipse I go!

- Start with a good old Java Application:
    - From the main menu choose **File > New > Java Project**
    - Enter the name for your plugin project.
    - Uncheck **Use default location**, and enter your *DEV* project, followed by '/' and the folder
        for your plugin.
    - Make sure a 1.8 JRE is chosen for the JRE.
    - Choose **Next**. The *Java Settings* page appears.
    - Click the **Libraries** tab.
    
    - Choose the **Add External JARs** button
        - Browse to the *DEV/jgnash_install/lib* folder
        - Select all the files in the folder and choose **OK**.
        
    - Choose the **Finish** button.

- Make sure your new project is selected in the *Package Explorer*
- Choose **File > New > Package**, and enter a package name for your plugin.
- Select the newly added package in the Package Explorer, and choose **File > New > Class**.
- The *New Java Class* window appears.
- Make sure the package is the package you created, then enter a name for your plugin class.
    This is the class that jGnash will look for when it tries to load your plugin.
- The new class should open up. Replace the contents with the following, leaving the package line at the top.
```
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import jgnash.plugin.FxPlugin;
import jgnash.uifx.views.main.MainView;

public class TestJGnashPlugin implements FxPlugin {

	@Override
	public String getName() {
		return "TestJGnashPlugin";
	}


    @Override
    public void start(PluginPlatform pluginPlatform) {
        if (pluginPlatform == PluginPlatform.Fx) {
            installFxMenu();
        }
    }
    
    private void installFxMenu() {
        final MenuBar menuBar = MainView.getInstance().getMenuBar();

        menuBar.getMenus().stream().filter(menu -> "fileMenu".equals(menu.getId())).forEach(menu -> menu.getItems()
                .stream().filter(menuItem -> menuItem instanceof Menu)
                .filter(menuItem -> "importMenu".equals(menuItem.getId())).forEach(menuItem -> {

                    final MenuItem importMenuItem = new MenuItem("Test JGnash Plugin");
                    ((Menu) menuItem).getItems().add(0, importMenuItem);
                }));
    }
}
```

- Save everything: Choose **File > Save All**
- Create a manifest file, jGnash will use this to locate the plugin class:
    - Choose **File > New > File**.
    - Select the top level folder of your project as the parent folder.
    - Enter *MANIFEST.MF* as the file name.
    - Choose **Finish** to create the file.
    - In the file itself, add the following:
```
Manifest-Version: 1.0
Plugin-Activator: testjgnashplugin.TestJGnashPlugin
Plugin-Version: 2.25
```

replacing *testjgnashplugin.TestJGnashPlugin* with the fully qualified name of your plugin class.

- Generate the JAR file for your plugin:
    - Choose **File > Export**, which brings up the *Export* window.
        - Select **Java > JAR file**
        - Choose **Next**.
    - The *JAR File Specification* window appears.
        - Check your project under *Select the resources to export*. At the least the right half window should
            display .classpath, .project, and MANIFEST.MF, all checked.
        - For *Select the export destination*, *JAR file*, choose **Browse**
            - Browse to the folder *jgnash_install/plugins* within your *DEV folder*
            - Enter the name for your plugin JAR file.
            - Choose **OK**.
        - Choose **Next**.
    - The *JAR Packaging Options* window appears.
        - Check *Save the description of this JAR in the workspace*, it will make things easier later.
        - Choose **Browse**.
            - Select your project as the parent folder.
            - Enter a name for the file, such as *JAR Export*.
            - Choose **OK**.
        - Choose **Next**.
    - The *JAR Manifest Specification* window appears.
        - Choose **Use existing manifest from workspace**.
        - Choose the **Browse** button after *Manifest file*.
            - Select *MANIFEST.MF* from your project.
            - Choose **OK**.
        - Choose **Finish**.

    - Eclipse doesn't say anything about the JAR file being successfully created, so verify it exists in your 
        **jgnash_install/plugins** folder.

- Create a Run configuration so you can launch jGnash from Eclipse:
    - Choose **Run > Run Configurations**. The *Run Configurations* window appears.
    - Select **Java Application**.
    - Choose the **New** button to create a new launch configuration.
        - Enter a name for the configuration, say *jGnash*.
        - Select the *Main* tab if it is not selected.
        - Choose the *Browse* button for Project.
            - Select your project.
            - Choose **OK**.
        - Choose the **Search** button for *Main class*.
            - The *Select Main Type* window appears.
            - Type *jgn* in Select type.
            - Matching items should display *jGnashFx - (default package)*.
            - With *jGnashFx - (default package)* selected, choose **OK**.
        - Choose the **Run** button, jGnash should launch.

- Hopefully jGnash launched successfully!
    - The Eclipse Console window will hopefuly display a line similar to:
> INFO: Starting plugin: yourplugin

- The ultimate test will be to open the **File > Import** menu in jGnash, it should show an item
        *Test JGnash Plugin*, which was added by the plugin.

At this point you need to do two things in order to get jGnash running with the plugin:
1 Build the JAR file.
2 Launch jGnash.

You can set things up to do both from one launch configuration. First you'll need to create an Ant build file to 
build the JAR file in the appropriate location.
- In *Package Explorer* select your project.
- Choose **File > New > Other**. The *Select a wizard* window appears.
    - Select *XML > XML File*
    - Choose **Next**.
    - The *Create a new XML file* window appears.
    - Select your project.
    - For *File name:* enter:
    > build.xml
    - Choose **Finish**.
- Open the new file *build.xml*, which should be at the root of your project, if it is not automatically opened.
- Enter the following in *build.xml*:
```
<?xml version="1.0" encoding="UTF-8"?>
<project name="GnuCashJGnash" default="CreateJar">
	<target name="CreateJar" description="Create Jar file">
		<jar jarfile="../jgnash_install/plugins/TestJGnashPlugin.jar" basedir="./bin" includes="**/*.class,**/*.properties" 
			manifest="MANIFEST.MF"/>
	</target>
</project>
```
replacing *TestJGnashPlugin* with the name for your JAR file.
- Save everything.
- Select *build.xml* in *Package Explorer*.
- Choose **File > Properties**. The *Properties for build.xml* window appears.
    - Select *Run/Debug Settings*
    - Choose **New...**. The *Select Configuration Type* window appears.
        - Select *Ant Build*
        - Choose **OK**. The *Edit Configuration* window appears.
            - Change *Name:* to *Build JAR*.
            - Choose **OK**.
    - You should be back in the *Properties for build.xml* window.
        - Choose **Apply and Close**.
        
    
- Choose **Run > Run Configurations...**. The *Run Configurations* window will appear.
    - Select *Launch Group*.
    - Choose the **New** button to create a new launch configuration.
        - Enter a name for the configuration, say *jGnash plugin*.
        - Choose **Add...**. The *Add Launch Configuration* window appears.
            - Select *Ant Build*, then *Build JAR*.
            - Choose **OK**.
        - Choose **Add...** again.
            - Select *Java Application*, then *jGnash*.
            - Choose **OK**
        - The *Launches* tab should show something like:
```
        Ant Biuld::Build JAR
        Java Application::jGnash
```
The order is important, as we want to build the JAR before launching jGnash...
To test it out:
    - First go to the *jgnash_install/plugins* folder and delete the JAR that had been created earlier. Don't delete jGnash' mt940 JAR, though!
    - Back in Eclipse, run the configuration. jGnash should launch, and your plugin should update the menus accordingly.
