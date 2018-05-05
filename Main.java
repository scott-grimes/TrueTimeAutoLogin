/**
 * 
 * This program is used to login to TrueTime, the Skyward timecard system
 * The user must specify their login credentials, and how long they would like to be
 * logged in for. 
 * 
 * Ever forget to log out of TrueTime? Tired of filling out timecard correction sheets?
 * 
 * The user will be logged in as soon as the program is loaded, and after the specified
 * length of time they will be logged out.
 * 
 * Created By: Scott Grimes
 * 
 * Requires Selenium / ChromeDriver
 */

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import java.util.Set;
import java.util.Iterator;
import java.time.LocalDateTime;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import java.io.*;
import java.time.DayOfWeek;


public class Main
{
    
    /**
     *  To Change User Settings, open the EDITME.txt file.
     *
     */
    
    //minutes to be logged in
    static String USERNAME = "";
    static String PASSWORD = "";
    
    //class vars
    static boolean DEBUG = false; //if this is flagged the user will not be logged in/out. used to test loading 
    static String[] windows; //array of window objects
    static boolean loggedIn;
    static LocalDateTime dateTime; //current time and date
    static int day; //day of the week: 0-sun, 1-mon, etc.
    
    //arrays representing when the user should be logged in/out on each day. array[0] is sunday, array[1] is monday, etc.
    static int[] logoutHour = new int[7];
    static int[] logoutMin = new int[7]; 
    static int[] loginHour = new int[7];
    static int[] loginMin = new int[7];
    static String[] loginType = new String[7]; //can be "NONE" for no login, "TUTOR" or "LEAD
    
    //current time
    static int hour;
    static int min;
    static int sec;
    
    
    static WebDriver driver;
    
    static int numOfWindowsOpen = 0; //used to set the focus of webdriver to popup winows
    
    
   public static void main(String args[]){
       
       dateTime = LocalDateTime.now();
       printWithTimeStamp("Press Control-C to exit the program");
       printWithTimeStamp("Loading Settings from EDITME.txt");
       if( !loadSettings())
        System.exit(0);
       printWithTimeStamp("Waiting for next Login Time: "+nextLoginTime());
       loggedIn = false;
        
        while(true){
            dateTime = LocalDateTime.now();
            day = dateTime.getDayOfWeek().getValue(); 
            min = dateTime.getMinute();
            hour = dateTime.getHour();
            
           
            
            if( DEBUG || !loggedIn && 
                !loginType[day].equals("NONE") && 
                hour>=loginHour[day] && min>=loginMin[day] && 
                !( hour>=logoutHour[day] && min>=logoutMin[day])){
                if( login())
                    loggedIn = true;
            }
            if( DEBUG || loggedIn && hour>=logoutHour[day] && min>=logoutMin[day]){
                if( logout() )
                {loggedIn = false;
                }
            }
            if(DEBUG)
                break;
                
             //only check once a minute
            if(!DEBUG)
                pauseFor(60000);
                
        }
    }
    
    //returns the next time a user will be logged in. If the user should be logged in right now, returns the current time.
    public static String nextLoginTime(){
        dateTime = LocalDateTime.now();
        day = dateTime.getDayOfWeek().getValue(); 
           min = dateTime.getMinute();
            hour = dateTime.getHour();
        boolean hasLogin = false;
       
        for(int i = day;i<day+7;i++){
            int currentDate = i%7;
            if(!loginType[currentDate].equals("NONE")){
                //same day should be logged in
                    
                    LocalDateTime loginDate = LocalDateTime.now();
                    loginDate = loginDate.plusDays(i-day);
                    loginDate = loginDate.withHour(loginHour[currentDate]);
                    loginDate = loginDate.withMinute(loginMin[currentDate]);
                    return loginDate.toString();
                
                    
            }
        }
        System.out.println("You have not set any day to login! Exiting Program");
        System.exit(0);
        return "No Login Date Found";
        
        
        
        
        
    }
    
    public static boolean loadSettings(){
       // chromedriver must be in current directory
        String currentDir = System.getProperty("user.dir");
        System.setProperty("webdriver.chrome.driver",currentDir+"\\chromedriver.exe");
        System.setProperty("java.library.path", currentDir);
        
        //set up our window variables
        // window 0, 1 and 2 are the skyward login screen, skyward popup, and truetime login code windows
        // the array is used to switch between each active window as needed
        windows = new String[3]; 
        
        try{
          File file = new File("EDITME.txt");
         
          BufferedReader br = new BufferedReader(new FileReader(file));
         
          String line;
          int lineNumber = -3;
          boolean hasAlogin = false;
          System.out.println("Will log in at the following times");
          while ((line = br.readLine()) != null){
            
            if(line.substring(0,2).equals("//"))
                continue;
            lineNumber++;    
            switch(lineNumber){
                case -2: USERNAME=line.split(" ",2)[1]; break;
                case -1: PASSWORD = line.split(" ",2)[1]; break;
                case 0:
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                case 6:
                // line looks like... TUESDAY TUTOR 16:00 17:30
                String[] parsed = line.split(" ");
                loginType[lineNumber] = parsed[1]; // stores "TUTOR"
                if( !parsed[1].equals("NONE")){
                    hasAlogin = true;
                    loginHour[lineNumber] = Integer.parseInt( parsed[2].split(":")[0] ); //Stores 16 (from 16:00)
                loginMin[lineNumber] = Integer.parseInt( parsed[2].split(":")[1] ); //Stores 00 (from 16:00)
                logoutHour[lineNumber] = Integer.parseInt( parsed[3].split(":")[0] ); //Stores 17 (from 17:30)
                logoutMin[lineNumber] = Integer.parseInt( parsed[3].split(":")[1] ); //Stores 30 (from 17:30)
                System.out.println(DayOfWeek.of(lineNumber)+" from "+loginHour[lineNumber]+":"+loginMin[lineNumber]+" to "+logoutHour[lineNumber]+":"+logoutMin[lineNumber]);
                }
                break;
                
            }
              
            
        }
        
        return hasAlogin;
        }
            catch(Exception e){
                System.out.println(e);
                
            }
            return false;
}
  
        
        
    
    
    /**
     * Logs the user into truetime. Default setting is Tutoring on Tuesdays and LEAD on Thursdays. Can be customized
     */
    public static boolean login(){
        
        //logs in to skyward, switches driver control to popup window
        if( !loginToSkywardFinance())
            return false;
        
        //opens the truetime menu
        navigateToTrueTime();
        
        //clicks the login button
        waitForId("IN");
        driver.findElement(By.id("IN")).click();
        if( !waitForNewWindow())
            return false;
        
        //Popup window asking what clock code to use (Tutoring, LEAD, curriculum writing, etc)
        //switches control of the driver to the new popup window
        Set<String> newWindowIterator = driver.getWindowHandles();
        for(String i: newWindowIterator){
            if(!i.equals(windows[0]) && !i.equals(windows[1])){
                driver.switchTo().window(i);
                    windows[2] = driver.getWindowHandle();
                    break;
            }
        }
        
        
        
        //customize your login for each day here. inspect the 'id' element of each checkbox and add a new day
        if(loginType[day].equals("TUTOR")) // login for tutoring
        {
            waitForId("0x0000000000f318b4check");
            driver.findElement(By.id("0x0000000000f318b4check")).click(); //Tutoring
        }
            
        else if( loginType[day].equals("LEAD") || DEBUG)//thursday - login for lead (also used when debugging)
            {
                waitForId("0x0000000000f31a08check");
                driver.findElement(By.id("0x0000000000f31a08check")).click(); //LEAD
          
            }
            
        //can be removed, used to visually check that the correct box was clicked.
        pauseFor(1000);
        
        // logs you in for the day (unless you are debugging!)
        waitForId("bSelect");
        if(!DEBUG)
            driver.findElement(By.id("bSelect")).click(); 
        
        pauseFor(5000);
        //close all our windows
        
        closeAllWindows();
        printWithTimeStamp("Login Successful");
        return true;
        
    }
    
    /**
     * Waits for the element with a given ID to appear on our current page
     */
    public static void waitForId(String id){
        WebDriverWait wait = new WebDriverWait(driver, 10000);
        wait.until(ExpectedConditions.visibilityOfElementLocated((By.id(id))));
        pauseFor(1000);
    }
    /**
     * navigates to the truetime sub menu
     */
    public static void navigateToTrueTime(){
        //driver is already set to the skyward finance popup
       
        waitForId("nav_TrueTime");
        driver.findElement(By.id("nav_TrueTime")).click();
        
        waitForId("nav_TrueTimeTracker");
        driver.findElement(By.id("nav_TrueTimeTracker")).click();
    }
    /**
     * Closes all chrome windows
     */
    public static void closeAllWindows(){
        Set<String> newWindowIterator = driver.getWindowHandles();
        for(String i: newWindowIterator){
            driver.switchTo().window(i);
            driver.close();
        }
    
    }
    /**
     * Logs the user out for the day
     */
    public static boolean logout(){
        
        //logs in to skyward, switches driver control to popup window
        if( !loginToSkywardFinance())
            return false;
        
        //opens the truetime menu
        navigateToTrueTime();
        
        
        //logs out
        waitForId("GONE");
        if(!DEBUG)
            driver.findElement(By.id("GONE")).click(); //Submit Login
        
        pauseFor(5000);
        //close our windows
        closeAllWindows();
        printWithTimeStamp("Logout Successful");
        return true;
    }
    /**
     * pauses this program for i number of miliseconds
     */
    public static void pauseFor(int i){
               try        
        {
            Thread.sleep(i);
        } 
        catch(InterruptedException ex) 
        {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * waits for a new popup window to be opened before continuing. 
     */
    public static boolean waitForNewWindow(){
        int i = 0;
        while(driver.getWindowHandles().size() <= numOfWindowsOpen){
            pauseFor(1000);
            if(i>30){
                printWithTimeStamp("could not open new window!");
                return false;
            }
            i++;
        }
        numOfWindowsOpen = driver.getWindowHandles().size();
        pauseFor(1000);
        return true;
    }
    /**
     * logs the user into skyward, transfers driver control to the new popup window
     */
    public static boolean loginToSkywardFinance(){
        
        driver = new ChromeDriver();
        
        String baseUrl = "https://skyward-finance.del-valle.k12.tx.us/scripts/wsisa.dll/WService=wsFin/seplog01.w";
        
        // launch Fire fox and direct it to the Base URL
        driver.get(baseUrl);
        
        waitForId("login");
        driver.findElement(By.id("login")).sendKeys(USERNAME); 
        
        waitForId("password");
        driver.findElement(By.id("password")).sendKeys(PASSWORD); 
        
        waitForId("bLogin");
        numOfWindowsOpen = driver.getWindowHandles().size();
        driver.findElement(By.id("bLogin")).click(); 
        if(!waitForNewWindow())
            return false;
        
        //find the new popup window, and switch the driver to handle it
        Set<String> windowIterator = driver.getWindowHandles();
        String orig = driver.getWindowHandle();
        windows[0] = orig;
        for(String i: windowIterator){
                if(!i.equals(orig)){
                    driver.switchTo().window(i);
                    windows[1] = driver.getWindowHandle();
                    break;
                }
        }
        return true;
    }
    
    public static void printWithTimeStamp(String s){
        dateTime = LocalDateTime.now();
            System.out.println("["+dateTime.toString()+"]: "+s);
        }
        
}
