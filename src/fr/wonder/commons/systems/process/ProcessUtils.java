package fr.wonder.commons.systems.process;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import fr.wonder.commons.files.FilesUtils;
import fr.wonder.commons.loggers.Logger;
import fr.wonder.commons.utils.Assertions;

public class ProcessUtils {
	
	public static final String[] SIGNALS = {
			"SIGHUP hangup", "SIGINT interrupt", "SIGQUIT terminal quit", "SIGILL illegal instruction",
			"SIGTRAP trap", "SIGABRT abort", null, "SIGFPE arithmetic error",
			"SIGKILL kill", null, "SIGSEGV segmentation fault", null,
			"SIGPIPE invalid pipe", "SIGALRM alarm clock", "SIGTERM termination", null
	};
	
	/** Calls {@link Thread#sleep(long)} and ignores interrupted exceptions */
	public static void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException x) {
		}
	}
	
	/** Creates a new thread that sleeps for {@code millis} ms and runs the given runnable */
	public static void runLater(Runnable r, long millis) {
		new Thread() {
			@Override
			public void run() {
				ProcessUtils.sleep(millis);
				r.run();
			}
		}.start();
	}
	
	/**
	 * Creates a new thread that reads from both process input and error streams and
	 * dumps them into the provided streams until the process exits or the thread is
	 * interrupted. IO errors are discarded while reading/writing from/to the
	 * process/output streams.<br>
	 * <br>
	 * This thread <b>must be started by the caller</b>. It is returned with a default
	 * name that can (and should) be changed before starting the redirection.
	 */
	public static Thread redirectOutput(Process process, OutputStream out, OutputStream err) {
		Thread t = new Thread("Process output redirect") {
			@Override
			public void run() {
				InputStream pin = process.getInputStream();
				InputStream perr = process.getErrorStream();
				while(process.isAlive()) {
					try {
						while(pin.available() > 0)
							out.write(pin.read());
						while(perr.available() > 0)
							err.write(perr.read());
						try {
							sleep(20);
						} catch (InterruptedException e) {
							return;
						}
					} catch (IOException x) { }
				}
				try {
					if(pin.available() > 0)
						pin.transferTo(out);
					if(perr.available() > 0)
						perr.transferTo(err);
					out.flush();
					err.flush();
				} catch (IOException x) { }
			}
		};
		return t;
	}
	
	public static Thread redirectOutputToStd(Process process) {
		return redirectOutput(process, System.out, System.err);
	}
	
	public static Thread redirectOutput(Process process, Logger logger) {
		return redirectOutput(process, logger.asStream(Logger.LEVEL_INFO), logger.asStream(Logger.LEVEL_ERROR));
	}
	
	public static String getErrorSignal(int exitValue) {
		int sig = (exitValue-1) ^ 0x80;
		if((exitValue & 0x80) == 0 || sig > 16)
			return null;
		return SIGNALS[sig];
	}
	
	public static void createLaunchBashScript(File file, String jarName) throws IOException {
		Assertions.assertNonNull(jarName);
		file.createNewFile();
		file.setExecutable(true);
		FilesUtils.write(file, "#!/bin/bash\n\njava -jar " + jarName + " $@");
	}
	
}
