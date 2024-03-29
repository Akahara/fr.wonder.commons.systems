package fr.wonder.commons.systems.process;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

import fr.wonder.commons.files.FilesUtils;
import fr.wonder.commons.loggers.Logger;
import fr.wonder.commons.types.ERunnable;
import fr.wonder.commons.types.ESupplier;
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

	/** Calls {@link Thread#join()} and converts interrupted exceptions to runtime exceptions */
	public static void join(Thread t) throws RuntimeException {
		try {
			t.join();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	/** <b>During development</b>, this may be used to run a line without catching checked errors */
	public static void silence(ERunnable<Throwable> callable) {
		try {
			callable.run();
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}
	
	/** <b>During development</b>, this may be used to run a line without catching checked errors */
	public static <T> T silence(ESupplier<T, Throwable> callable) {
		try {
			return callable.get();
		} catch (Throwable e) {
			throw new RuntimeException(e);
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
				try {
					redirectOutputSync(process, out, err);
				} catch (InterruptedException x) { }
			}
		};
		return t;
	}
	
	public static void redirectOutputSync(Process process, OutputStream out, OutputStream err) throws InterruptedException {
		InputStream pin = process.getInputStream();
		InputStream perr = process.getErrorStream();
		InterruptedException interuption = null;
		while(process.isAlive()) {
			try {
				while(pin.available() > 0)
					out.write(pin.read());
				while(perr.available() > 0)
					err.write(perr.read());
				try {
					Thread.sleep(20);
				} catch (InterruptedException e) {
					interuption = e;
					break;
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
		
		if(interuption != null)
			throw interuption;
	}

	public static Thread redirectOutputToStd(Process process) {
		return redirectOutput(process, System.out, System.err);
	}
	
	public static void redirectOutputSyncToStd(Process process) throws InterruptedException {
		redirectOutputSync(process, System.out, System.err);
	}
	
	public static Thread redirectOutput(Process process, Logger logger) {
		return redirectOutput(process, logger.asStream(Logger.LEVEL_INFO), logger.asStream(Logger.LEVEL_ERROR));
	}
	
	public static void redirectOutputSync(Process process, Logger logger) throws InterruptedException {
		redirectOutputSync(process, logger.asStream(Logger.LEVEL_INFO), logger.asStream(Logger.LEVEL_ERROR));
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
	
	public static boolean extractFileFromResources(String resourcePath, File extractedPath) throws IOException {
		if(!resourcePath.startsWith("/"))
			throw new IllegalArgumentException("Resource paths should with a /");
		if(extractedPath.exists())
			return false;
		try (InputStream is = ProcessUtils.class.getResourceAsStream(resourcePath)) {
			Files.copy(is, extractedPath.toPath());
		}
		return true;
	}
	
	public static void loadDLL(String absolutePathToDLL) {
		System.load(absolutePathToDLL);
	}

	public static void extractAndLoadDLL(String resourcePath, File extractedPath) throws IOException {
		extractFileFromResources(resourcePath, extractedPath);
		loadDLL(extractedPath.getAbsolutePath());
	}
	
}
