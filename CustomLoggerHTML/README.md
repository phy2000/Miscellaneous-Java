# CustomLoggerHTML
This is an extension to the log4j HTMLLayout module.<br>
The standard module shows timestamps as the number of 
milliseconds since the start of logging.<br>
This extension shows the time as <b>yyyy-MM-dd-HH:mm:ss.SZ</b>, for example <b>2008-11-21-18:35:21.472-0800</b><br>
<hr>
The standard module is set in the log4j.properties file: <br>
log4j.appender.FILE.layout=org.apache.log4j.HTMLLayout <br>
<hr>
To use this custom layout, export to a jar, place the jar in the classpath, and add this to the <b>log4j.properties</b> file:<br>
<b>log4j.appender.FILE.layout=infa.presales.custom.HtmlLayoutExtension</b> <br>
