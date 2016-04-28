/*
 * Copyright (c) 2002-2010 ESP Suite. All Rights Reserved.
 *
 *     
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR 
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE 
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, 
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. 
 * 
 * Authors: Technology Integration Group, SCE
 * Developers: Eddie Mayfield, Frank Gonzales, Augustin Muniz,
 * Kate Suwan, Hiro Kushida, Andrew McNaughton, Brian Stonerock,
 * Russell Ragsdale, Patrick Ridge, Everett Aragon.
 * 
 */
package com.digitald4.common.log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Writer;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.JFileChooser;
import javax.swing.ListModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

/**
 * This class provides methods for logging events. In terms of functionality,
 * it is somewhere in between <code>System.out.println()</code> and
 * full-blown logging packages such as log4j.<p>
 *
 * All events are logged to an in-memory buffer and optionally a stream,
 * and those with a high urgency (warnings and errors) are also printed
 * to standard output.<p>
 *
 * Logging of exception tracebacks is supported.<p>
 *
 * This class can also optionally redirect standard output and error to the log.
 *
 */
public class EspLogger
{
	//{{{ Constants
	/**
	 * The maximum number of log messages that will be kept in memory.
	 */
	public static final int MAXLINES = 500;
	
	public static enum LEVEL{DEBUG,MESSAGE,NOTICE,WARNING,ERROR};
	
	//}}}
	//{{{ Private members

	//{{{ Instance variables
	private static Object LOCK = new Object();
	private static String[] log;
	private static int logLineCount;
	private static boolean wrap;
	private static LEVEL level = LEVEL.MESSAGE;
	private static Writer stream;
	private static String lineSep;
	private static PrintStream realOut;
	private static PrintStream realErr;
	private static LogListModel listModel;
	//}}}
	//{{{ Class initializer
	static{
		realOut = System.out;
		realErr = System.err;
		log = new String[MAXLINES];
		lineSep = System.getProperty("line.separator");
		listModel = new LogListModel();
	} //}}}

	//{{{ init() method
	/**
	 * Initializes the log.
	 * @param stdio If true, standard output and error will be
	 * sent to the log
	 * @param level Messages with this log level or higher will
	 * be printed to the system console
	 */
	public static void init(boolean stdio, LEVEL level){
		if(stdio){
			if(System.out == realOut && System.err == realErr){
				System.setOut(createPrintStream(LEVEL.MESSAGE,null));
				System.setErr(createPrintStream(LEVEL.ERROR,null));
			}
		}

		EspLogger.level = level;

		// Log some stuff

		log(LEVEL.MESSAGE,EspLogger.class,"***************************************************************");
		log(LEVEL.MESSAGE,EspLogger.class,"              ESP Suite LOG Output							 ");
		log(LEVEL.MESSAGE,EspLogger.class,"***************************************************************");
		log(LEVEL.MESSAGE,EspLogger.class,"Log file created on " + new Date());
		log(LEVEL.MESSAGE,EspLogger.class,"");
		String[] props = {
				"java.version", "java.vm.version", "java.runtime.version",
				"java.vendor", "java.compiler", "os.name", "os.version",
				"os.arch", "user.home", "java.home",
		};
		for(int i = 0; i < props.length; i++)
			log(LEVEL.MESSAGE,EspLogger.class,	props[i] + "=" + System.getProperty(props[i]));
	} //}}}

	//{{{ setLogWriter() method
	/**
	 * Writes all currently logged messages to this stream if there was no
	 * stream set previously, and sets the stream to write future log
	 * messages to.
	 * @param stream The writer
	 */
	public static void setLogWriter(Writer stream){		
		if(EspLogger.stream == null && stream != null){
			try{
				if(wrap){
					for(int i = logLineCount; i < log.length; i++){
						stream.write(log[i]);
					}
				}
				for(int i = 0; i < logLineCount; i++){
					stream.write(log[i]);
				}
				stream.flush();
			}
			catch(Exception e){
				// do nothing, who cares
			}
		}		
		EspLogger.stream = stream;
	} //}}}
	
	public static LEVEL getLevel(){
		return level;
	}
	public static void setLevel(LEVEL debugLevel){
		level=debugLevel;
	}

	//{{{ flushStream() method
	/**
	 * Flushes the log stream.
	 */

	public static String getLogStream(){
		String temp ="";
		for(int i=0;i<logLineCount;i++)
			temp=temp+log[i]+"\r\n";		    					//separate each message line by adding end line to the end	  
		return temp;		
	}	

	////////////////////	
	public static void makeLogFile(){	
		String logFileName = "\\log.txt";								//Create log.txt on user's computer
		JFileChooser fc = new JFileChooser();
		System.out.println("path: "+fc.getSelectedFile());
		int returnVal = fc.showSaveDialog(fc);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			File logFile = new File(fc.getSelectedFile()+logFileName);	//file path
			try {
				FileOutputStream fop=new FileOutputStream(fc.getSelectedFile().getPath()+logFileName);			
				if(logFile.exists()){
					for(int i=0;i<logLineCount;i++){
						int counter=300, lineLength=300;		    		  
						while(log[i].length()-counter>lineLength || (log[i].length()-counter>0)){	//long line will be cut into shorter lines
							log[i]=log[i].substring(0,counter)+"\r\n"+log[i].substring(counter); 
							counter=counter+lineLength+2;		    			  
						}
						log[i]=log[i]+"\r\n";		    					//separate each message line by adding end line to the end	  
						fop.write(log[i].getBytes());						//write each message to mem pool
					}
					fop.flush();											//write messages to file
					fop.close();											//close stream
				}
				else   System.out.println("This file is not exist"); 
			} catch (FileNotFoundException e1) {						//Check errors
				e1.printStackTrace();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}
	
	public static void flushStream(){
		if(stream != null){
			try{
				stream.flush();
			}
			catch(IOException io){
				io.printStackTrace(realErr);
			}
		}
	} //}}}

	//{{{ closeStream() method
	/**
	 * Closes the log stream. Should be done before your program exits.
	 */
	public static void closeStream(){
		if(stream != null){
			try{
				stream.close();
				stream = null;
			}
			catch(IOException io){
				io.printStackTrace(realErr);
			}
		}
	} //}}}

	//{{{ getLogListModel() method
	/**
	 * Returns the list model for viewing the log contents.
	 */
	public static ListModel getLogListModel(){
		return listModel;
	} 

	//{{{ log() method
	/**
	 * Logs a message. This method is thread-safe.<p>
	 *
	 * The following code sends a typical debugging message to the activity
	 * log:
	 * <pre>Log.log(Log.DEBUG,this,"counter = " + counter);</pre>
	 * The corresponding activity log entry might read as follows:
	 * <pre>[debug] JavaParser: counter = 15</pre>
	 *
	 * @param urgency The urgency; can be one of
	 * <code>Log.DEBUG</code>, <code>Log.MESSAGE</code>,
	 * <code>Log.NOTICE</code>, <code>Log.WARNING</code>, or
	 * <code>Log.ERROR</code>.
	 * @param source The source of the message, either an object or a
	 * class instance.
	 * @param message The message. This can either be a string or
	 * an exception
	 *
	 */	
	public static void log(LEVEL urgency, Object source, Object message){
		String _source;
		if(source == null){
			_source = Thread.currentThread().getName();
			if(_source == null){
				_source = Thread.currentThread().getClass().getName();
			}
		}
		else if(source instanceof Class)
			_source = ((Class<?>)source).getName();
		else
			_source = source.getClass().getName();
		int index = _source.lastIndexOf('.');
		if(index != -1)
			_source = _source.substring(index+1);

		if(message instanceof Throwable){
			_logException(urgency,source,(Throwable)message);
		}
		else{
			String _message = String.valueOf(message);
			// If multiple threads log stuff, we don't want
			// the output to get mixed up
			synchronized(LOCK){
				StringTokenizer st = new StringTokenizer(
						_message,"\r\n");
				int lineCount = 0;
				boolean oldWrap = wrap;
				while(st.hasMoreTokens())
				{
					lineCount++;
					_log(urgency,_source,st.nextToken()
							.replace('\t',' '));
				}
				listModel.update(lineCount,oldWrap);
			}
		}
	} //}}}

	public static int getLogLineCount(){
		return logLineCount;
	}

	public static String[] getLog(){
		return log;
	}

	//{{{ createPrintStream() method
	private static PrintStream createPrintStream(final LEVEL urgency,
			final Object source)
	{
		return new PrintStream(new OutputStream() {
			public void write(int b){
				byte[] barray = { (byte)b };
				write(barray,0,1);
			}

			public void write(byte[] b, int off, int len){
				String str = new String(b,off,len);
				log(urgency,source,str);
			}
		});
	} //}}}

	//{{{ _logException() method
	private static void _logException(LEVEL urgency,
			final Object source,
			final Throwable message)
	{
		PrintStream out = createPrintStream(urgency,source);

		synchronized(LOCK){
			message.printStackTrace(out);
		}
	} //}}}

	//{{{ _log() method
	private static void _log(LEVEL urgency, String source, String message){
		
		if(urgency.ordinal() >= level.ordinal()){
			String fullMessage = "[" + urgencyToString(urgency) + "] " + source+ ": " + message;		
			try{
				log[logLineCount] = fullMessage;
				if(++logLineCount >= log.length){
					wrap = true;
					logLineCount = 0;
				}
				if(stream != null){
					stream.write(fullMessage);
					stream.write(lineSep);
				}
			}
			catch(Exception e){
				e.printStackTrace(realErr);
			}
		
			if(urgency == LEVEL.ERROR)
				realErr.println(fullMessage);
			else
				realOut.println(fullMessage);
		}
	} //}}}

	//{{{ urgencyToString() method
	private static String urgencyToString(LEVEL urgency){
		switch(urgency){
			case DEBUG:			return "debug";
			case MESSAGE:		return "message";
			case NOTICE:		return "notice";
			case WARNING:		return "warning";
			case ERROR:			return "error";
		}
		throw new IllegalArgumentException("Invalid urgency: " + urgency);
	} //}}}
	//{{{ LogListModel class
	static class LogListModel implements ListModel {
		Vector<ListDataListener> listeners = new Vector<ListDataListener>();
		private void fireIntervalAdded(int index1, int index2){
			for(ListDataListener listener : listeners) {
				listener.intervalAdded(new ListDataEvent(this,
						ListDataEvent.INTERVAL_ADDED,
						index1,index2));
			}
		}

		private void fireIntervalRemoved(int index1, int index2){
			for(ListDataListener listener : listeners) {
				listener.intervalRemoved(new ListDataEvent(this,
						ListDataEvent.INTERVAL_REMOVED,
						index1,index2));
			}
		}

		public void addListDataListener(ListDataListener listener){
			listeners.addElement(listener);
		}

		public void removeListDataListener(ListDataListener listener){
			listeners.removeElement(listener);
		}

		public String getElementAt(int index){
			if(wrap){	
				if(index < MAXLINES - logLineCount)					
					return log[index + logLineCount];
				return log[index - MAXLINES + logLineCount];
			}
			return log[index];
		}

		public int getSize(){
			if(wrap)		
				return MAXLINES;
			return logLineCount;
		}

		void update(final int lineCount, final boolean oldWrap){
			if(lineCount == 0 || listeners.size() == 0)
				return;

			SwingUtilities.invokeLater(new Runnable(){
				public void run(){
					if(wrap){
						if(oldWrap)		fireIntervalRemoved(0,lineCount - 1);
						else			fireIntervalRemoved(0,logLineCount);						
						fireIntervalAdded(MAXLINES - lineCount + 1,	MAXLINES);
					}
					else	fireIntervalAdded(logLineCount - lineCount + 1,	logLineCount);					
				}
			});
		}
	}
	
	/**
	 * log a ERROR message
	 * @param source the source class
	 * @param strMsg the string message
	 */
	public static void error(Object source, Object strMsg){
		log(LEVEL.ERROR, source, strMsg);
	}

	/**
	 * log a WARNING message
	 * @param source the source class
	 * @param strMsg the string message
	 */
	public static void warning(Object source, Object strMsg){
		log(LEVEL.WARNING, source, strMsg);
	}

	/**
	 * log a NOTICE message
	 * @param source the source class
	 * @param strMsg the string message
	 */
	public static void notice(Object source, Object strMsg){
		log(LEVEL.NOTICE, source, strMsg);
	}
	
	public static void info(Object strMsg){
		log(LEVEL.NOTICE,null, strMsg);
	}

	/**
	 * log a DEBUG message
	 * @param source the source class
	 * @param strMsg the string message
	 */
	public static void debug(Object source, Object strMsg){
		log(LEVEL.DEBUG, source, strMsg);
	}
	
	/**
	 * log a DEBUG MESSAGE
	 * @param source the source class
	 * @param strMsg the string message
	 */
	public static void message(Object source, Object strMsg){
		log(LEVEL.MESSAGE, source, strMsg);
	}
}
