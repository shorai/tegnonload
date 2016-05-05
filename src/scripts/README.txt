This folder contains all the runtime components that define the tegnonEfficiency runtime

In order to install on a new system, simply 

	- sync this folder where you require it
	- adjust the local directory names to match what you have
	- create scheduler tasks to kick off the processes as required
	     run.bat gets triggered each half hour, 4 minutes past the hour  
		 the rest should be obvious
		 in Microsoft go to start->admintools->taskScheduler
		    The only gotch is that for run.bat you create a task to run every hour, 
			    then modify its properties to run every half hour , whatever
    -The scripts/Version2 folder contains extractors to install the gmail fetch program				

COMPLETE INSTALL
================

Install Process to get Tegnon working on Amazon
===============================================
     FOR Disaster Recovery see the bottom of this script

1) Create IAM users and accounts for each user in both Admin and User 
         - Standard name.surname == admin user
                    name         == standard user
         - Create certificates and download

2) Create an Elastic IP

3) Create Directory Service (if using windows) for Active Directory logins

4) Check you have a valid and updated image of Database and Webserver on S3 storage 
         Download S3 browser app and run it to transfer data files
         snapshot any servers
         zip and upload wwwroot folder

Get Latest Copies of Utilities
==============================

The utilities we use are numerous, get them firss, and run the installers.
Always prefix a new install with these


a) AWS Tools 

	Command Line (CLI)  
		http://aws.amazon.com/cli    
		- command line utilities to interact with amazon NB for backing up to and restoring from s3
	S3 browser
		http://s3browser.org
		A bit dated and does not manage Glacier storage (yet)

b) 7 Zip   :               http://7z.org
	Windows does not supply a command line zip. Saves storage costs

c) TIBCO JasperSoft Studio
        For creating reports

d) Jasper Reports server

e) IIS extensions
	IIS Web Platform Installer https://www.microsoft.com/web/downloads/platform.aspx
              Then use it to install latest PHP and SQL drivers for PHP 

	ARR  Microsoft IIS Application Request Routing
		https://www.microsoft.com/en-us/download/details.aspx?id=35838
		Provides reverse proxy capabilities to IIS, Enables us to reverse proxy jasper reports
	
	vcredist_x64 and dotNetFx40_client_setup
	        Microsoft - probably came down automatically as part of another package

f) Notepad++
      Strictly not required but a better text editor

g) mysql-installer 
      If using mysql

h) Composer-setup
      Install manager used by Laravel 

i) jtds
      https://sourceforge.net/jtds
      Alternate java to SQL Server  jdbc driver set.
      sqljdbc is the Microsoft tool, dated (may 2016) and not well supported
 
j) Netbeans
      www.netbeans.org 
      A comfortable java project IDE, you could use Jasper Report Studio in java mode
      GEt the full version with JDK etc

k) git
     www.git.org
     Version control of all scripts

l) MySQL and MysqlWorkbench
     www.mysql.org
     Server and useful IDE for sql
     can also look at php mysql administrator

m) Windirstat
	https://windirstat.info
       Nice graphical manager of space on your hard drive

n) Source code for the java load program (tegnonload.jar) can be found in the private GIT account attached to this AWS account


Integrated Web and SQL Server
=============================
This is the simplest approach, least flexible and scalable

1) Create a new ECS from the built in AMI for Microsoft and SQL server
2) attach Elastic IP and security domain
3) start the server

4) Setup Internet Explorer for file downloads
     Go to settings->security->advanced ->file download. Enable

5) Download the additional packages you require
	There are only a few packs, so put them on the desktop for easy access

      -- IIS Web Platform Installer https://www.microsoft.com/web/downloads/platform.aspx
              Then install latest PHP and SQL drivers for PHP 
      -- Composer
              Only if you are going to update Laravel
      -- Notepad++
      -- Netbeans
            GEt the latest from ORACLE (includes JDK where netbeans.org does not)
            Not sure that NB works over RAS ... seems very slow or is that the Internet??
      -- or Eclipse (for BIRT reporting)
      -- s3-browser
            Makes it much easier and faster
      -- windirstat 

6) Check php and IIS are running
         <?php phpinfo(); ?>
         Run and remove!!

7) Restore Database
	Open SQL Administrator
        Connect to the instance
        Right Click the Server (Top line with Windows machine name)
              properties->security
              Enable mixed mode logins (sql server and Windows)
	Click Database->RestoreDatabase->From Device(radio button on page)
                      Push the ... button to see the file folder          

8) Get your files from s3 and unzip them
       - wwwroot backup
                - zip and save a copy of c:\inetpub\wwwroot on the desktop
                - overwrite it with the zip contents
                        You can also unzip elsewhere and create a virtual directory in IIS

                - rename or delete iisstart.html
       - sql backup file (xxxxx.bak)
                unzip and move to c:\programFiles\sql...... \backups
		This path is in the SQLadministrator above

9) Restore the Database
       This will restore all data, procedures, views etc
       It also restores the Users for your DB (TegnonEfficiency)

10) Recreate SQL logins
       - Edit iis->wwwroot->apps->config->database.php
              Check the credentials and change password (good to have a different password)

       - You may also want to change the randomizer in app->config->env

       - in SQL administrator 
            WIN....->Security(machine security, not TegnonEfficiency security)
            Add login for the phpSQLUser
                LEAVE THE SCHEMA BLANK (you cannot erase it later)
            Add login for javaUser
	    Add logins for any users

11) Change File permissions for PHP laravel and Bootstrap
         technically you need IIS_USR to be able to read and write two folders (recursive)
	practically I simply add Everyone access to these folders since some other user sems to be used

       - wwwroot/app/storage and subfolders
       - wwwroot/bootstrap/cache
               
12) Check everything is working by browsing to the box

13) Email uploader
      Currently you need Version2 software (service) to fetch emails to c:\Tegnon\....gmail\....
      Install and start automatically
      In Google you may move files that were not processed from PROCESSED to inbox
            - The load may crash every 700 files or so, restart it in service manager 
	    - (it wont restart itself)

14) Java ETL
      Upload the TegnonLoad.jar file and associated lib dir with mssql42.jar 
      -- run it
      -- in future this will be a lambda function

15) Create Windows accounts for each user that requires a login
        Differentiate administative and user accounts as a security precaution
        Java ETL should be run as a scheduled task for a windows user with SQL privilege

16) Backup the database daily and move to Glacier storage
        or run RDS 


17) Microsoft server 2000
=========================
     a) Install sql server libs in c:\jars\mssql
     b) Copy the Tegnonload.jar and lib folder containing sql42.jar to c:\jars
             you may also need to point PATH to the dll files in the MS distro

     c) Configure the following scripts to run.
        best place is in a scheduler directory in Documents i.e c:\Users\..\Documents\scheduler
	tegnonload.sql
		USE [TegnonEfficiency]
		GO

		DECLARE	@recreateSensorDataHour int
		DECLARE	@sp_recreateSpecificPower int

		EXEC	@recreateSensorDataHour = [dbo].[sp_recreateSensorDataHour]

		EXEC	@sp_recreateSpecificPower = [dbo].[sp_recreateSpecificPower]

		SELECT	'SensorDataHour' @recreateSensorDataHour , 'RecreatepecificPower' @sp_recreateSpecificPower

		GO

     d) run.bat
		java   -jar TegnonLoad.jar
		sqlcmd -d TegnonEfficiency -U javaUser1 -P hghufd8764f89 -i tegnonload.sql




     e) from AdminTasks->scheduler create a new basic task to run daily
          Once created, go into properties and configure
                - trigger every 30 minutes tat 4 minutes and 34 minutes
                - run even when not logged in , provide a user with sufficient priveilege
		- you will need to provide a password (this user should be set up in MS to have sql authentication, you can then take out the -U and -P parameters
                - set the 'run in' parameter to c:\jars



18) Jasper Reports

        a) Go to http://community.jaspersoft.com  Login (using facebook or Google)
        b) Download Jasper server, look for the installer,exe
        c) Install
        d) change passwords from users->EDIT (button @ bottom of page)
        e) copy sql server jar file to tomcat/libs
        f) Restart and make a connection (prefer JNDI connection pool)
        g) Browse and check the demo reports are working
        h) in AWS manager,. open up port 8080 to the box 
        i) Download the latest Eclipse report writer 
                 - it may be possible to run this from a separate dev box


19) Fully automated BAckup process
        - Java logs from load process run for a day
       - Documents/jars/daily.bat file contains 
 		SQL backup and truncate log
		Zip all load logs
		Zip the TegnonLoad.jar and its libs
		TODO: add config files for the scheduler
		TODO: zip inetPub 
		TODO copy to s3 storage as a singl extract
		TODO delete old s3 versions
		TODO send a copy to glacier every 3 months


	- Want to image the entire machine once per week
             An Amazon snapshot of the elastic volume will suffice
		Keep 3 copies - Weekly & overridden * 2
                Monthly       - 
             Service can be restored in minutes
                Simply need to load past few day's transactions

20) Jasper recovery
       Logins are stored where ?
       Create reverse proxy in IIS to point to the Jasper server
             see IIS setup for details
       Reload logins
       Reload reports (from user account that has Jasper Studio)
       Need to create a new Login to Databsae
              The credentials are stored in clear text in the Jasper filesystem
                  can it use Windows authentication on SQL Server
                  Does Mysql introduce a vulnerability?
		Jasper report scripts are maintained in the user workspace folder as per any eclipse project
		
21) Version 2

         A mail retrieval program is currently being used , the extractor/installer is in the 
		       csripts/version2 folder. It neads to be run
	     Create a path C:\Tegnon\tegnonefficiencydatagmail.com\za.tegnon.consol@gmail.com
		        for it to work in


21) Disaster Recovery
====================
	First Line
        ----------      
	Encrypted images of the machine are created every week using the Amazon EBS imaging program
      
	At least one of these should be readable and usable, recover it and run it

        In the event that the server was compromised, at least the latest copy may also be compromised

        In either event it is probably worthwhile doing a fresh install (Second line) of everything
	    Follow these instructions from the top and note changes / improvements
	    Remember to create fresh passwords for each authentication point.
            Then load the latest daily backup from SQL server and rerun the latest transactions


22) Daily and Weekly monitoring
===============================
	Server must be monitored daily until stable
	Thereafter weekly should suffice

	- Disk Space
	- Log file growth 
        - Index physical usage and re-indexing
        - Failures in the load logs
        - Memory and CPU utilisation and spare capacity in the server(s)
        - Failed login attempts (Server, IIS, SQL, Application(s), Report Writer)
        - Successful logins (Server, IIS, SQL, Application(s), Report Writer)
	- Daily procedures (SQL backup, Disk imaging, Failure logs) 
	- Windows Event Viewer


 





				