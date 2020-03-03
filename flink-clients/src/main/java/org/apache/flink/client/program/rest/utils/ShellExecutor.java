package org.apache.flink.client.program.rest.utils;

import org.apache.commons.collections.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ShellExecutor {
	private static Logger LOG = LoggerFactory.getLogger(ShellExecutor.class);

	public static boolean executeCmd(String cmd) {
		try {
			CommandExcuteThread commandExcuteThread = new CommandExcuteThread(cmd);
			ExecutorUtils.submitTask(commandExcuteThread);
			while (!commandExcuteThread.finish) {
				LOG.info("shell:" + cmd + " is not finish");
				Thread.sleep(60 * 1000);
			}
			if (commandExcuteThread.exitValue != 0) {
				LOG.info("shell " + cmd + " execute fail,exitValue=" + commandExcuteThread.exitValue);
				return false;
			}
			return true;
		} catch (Exception e) {
			LOG.info("exucute cmd:" + cmd + " failed", e);
		}
		return false;
	}


	public static class CommandExcuteThread extends Thread {
		private String cmd;

		private boolean finish = false;

		private int exitValue = -1;

		private boolean timeout;

		public String getCmd() {
			return cmd;
		}

		public boolean isFinish() {
			return finish;
		}

		public int getExitValue() {
			return exitValue;
		}

		public boolean isTimeout() {
			return timeout;
		}

		public CommandExcuteThread(boolean timeout, String cmd) {
			this.timeout = timeout;
			this.cmd = cmd;
		}

		public CommandExcuteThread(String cmd) {
			this.cmd = cmd;
			this.timeout = false;
		}

		public void run() {
			Map<String, Object> result = new HashMap<>();
			result = runProcess(cmd, timeout, 10L, TimeUnit.MINUTES);
			this.exitValue = MapUtils.getIntValue(result, "exitValue", -1);
			finish = true;
		}


		public static Map<String, Object> runProcess(String cmd, boolean timeout, Long max, TimeUnit timeUnit) {
			Map<String, Object> map = new HashMap<>();
			StringBuilder infoLog = new StringBuilder();
			StringBuilder errorLog = new StringBuilder();
			try {
				Process process = Runtime.getRuntime().exec(cmd);
				BufferedReader infoInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
				BufferedReader errorInput = new BufferedReader(new InputStreamReader(process.getErrorStream()));
				String line = "";

				LOG.info("start execute shell:" + cmd);
				while ((line = infoInput.readLine()) != null) {
					infoLog.append(line).append("\n");
					LOG.info(line);
				}

				while ((line = errorInput.readLine()) != null) {
					errorLog.append(line).append("\n");
					LOG.info(line);
				}
				infoInput.close();
				errorInput.close();
				if (timeout) {
					process.waitFor(max, timeUnit); //阻塞脚本直到脚本执行完返回
				} else {
					process.waitFor();
				}
				map.put("exitValue", process.exitValue());
				LOG.info("execute shell:" + cmd + " finish");
			} catch (Exception e) {
				LOG.info("CommandExecuteThread occure exception,shell:" + cmd, e);
				map.put("exitValue", 100);
			}
			LOG.info("info log for cmd:" + cmd + ":" + infoLog);
			LOG.info("error log for cmd:" + cmd + ":" + errorLog);
			return map;
		}
	}
}
