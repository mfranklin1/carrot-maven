/**
 * Copyright (C) 2010-2012 Andrei Pozolotin <Andrei.Pozolotin@gmail.com>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.carrotgarden.maven.aws;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.slf4j.Logger;
import org.slf4j.impl.MavenLoggerFactory;

/**
 */
public abstract class CarrotAws extends AbstractMojo {

	/** do not use during class init */
	protected Logger getLogger(final Class<?> klaz) {
		return MavenLoggerFactory.getLogger(klaz, getLog());
	}

	/**
	 * @readonly
	 * @required
	 * @parameter expression="${project}"
	 */
	protected MavenProject project;

	/**
	 * @readonly
	 * @required
	 * @parameter expression="${settings}"
	 */
	protected Settings settings;

	//

	protected boolean isValidDirectory(final File file) {

		if (file == null) {
			return false;
		}

		if (!file.exists()) {
			return false;
		}

		if (!file.isDirectory()) {
			return false;
		}

		if (!file.canRead()) {
			return false;
		}

		if (!file.canWrite()) {
			return false;
		}

		return true;

	}

	protected Map<String, String> safeMap(final Properties props) {

		final Map<String, String> map = new HashMap<String, String>();

		if (props == null) {
			return map;
		}

		for (final Map.Entry<Object, Object> entry : props.entrySet()) {
			final String key = entry.getKey().toString();
			final String value = entry.getValue().toString();
			map.put(key, value);
		}

		return map;

	}

	protected Map<String, String> safeMap(final Map<String, String> props) {

		final Map<String, String> map = new HashMap<String, String>();

		if (props == null) {
			return map;
		}

		return props;

	}

	protected Properties load(final File file) throws Exception {

		final Properties props = new Properties();

		if (file == null || !file.exists()) {
			getLog().debug("file == null || !file.exists()");
			return props;
		}

		final Reader reader = new FileReader(file);

		props.load(reader);

		return props;

	}

	protected void save(final Properties props, final File file)
			throws Exception {

		if (props == null || file == null) {
			getLog().debug("props == null || file == null");
			return;
		}

		final File folder = file.getParentFile();

		if (!folder.exists()) {
			folder.mkdirs();
		}

		final Writer writer = new FileWriter(file);

		props.store(writer, null);

	}
}
