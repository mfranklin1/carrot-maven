/**
 * Copyright (C) 2010-2012 Andrei Pozolotin <Andrei.Pozolotin@gmail.com>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.carrotgarden.maven.aws.cfn;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.maven.settings.Server;
import org.slf4j.Logger;

import com.carrotgarden.maven.aws.CarrotAws;

/**
 * 
 */
public abstract class CarrotAwsCloudForm extends CarrotAws {

	/**
	 * AWS CloudFormation stack name; must be unique under your aws account
	 * 
	 * @required
	 * @parameter default-value="amazon-builder"
	 */
	protected String stackName;

	/**
	 * AWS CloudFormation
	 * 
	 * <a href=
	 * "http://docs.amazonwebservices.com/AWSSecurityCredentials/1.0/AboutAWSCredentials.html"
	 * >amazon security credentials</a>
	 * 
	 * stored in
	 * 
	 * <a href=
	 * "http://www.sonatype.com/books/mvnref-book/reference/appendix-settings-sect-details.html"
	 * >maven settings.xml</a>
	 * 
	 * under server id entry; username="Access Key ID",
	 * password="Secret Access Key";
	 * 
	 * @required
	 * @parameter default-value="com.example.aws.stack"
	 */
	protected String stackServerId;

	/**
	 * AWS CloudFormation operation timeout; seconds
	 * 
	 * @required
	 * @parameter default-value="600"
	 */
	protected Long stackTimeout;

	//

	protected CloudFormation getCloudFormation(final File templateFile,
			final Properties inputProps, final Map<String, String> inputParams)
			throws Exception {

		/** merge template parameters */

		final Map<String, String> stackParams = new HashMap<String, String>();

		/** from properties file */
		stackParams.putAll(safeMap(inputProps));

		/** from maven pom.xml */
		stackParams.putAll(safeMap(inputParams));

		/** */

		final String stackTemplate = safeTemplate(templateFile);

		/** */

		final Server server = settings.getServer(stackServerId);

		if (server == null) {
			throw new IllegalArgumentException(
					"settings.xml : server definition is missing for serverId="
							+ stackServerId);
		}

		final String username = server.getUsername();
		final String password = server.getPassword();

		/** */

		final Logger logger = getLogger(CloudFormation.class);

		/** */

		final CloudFormation formation = new CloudFormation(logger, stackName,
				stackTemplate, stackParams, stackTimeout, username, password);

		return formation;

	}

	protected String safeTemplate(final File templateFile) throws Exception {
		if (templateFile == null || !templateFile.exists()) {
			return "{}";
		} else {
			return FileUtils.readFileToString(templateFile);
		}
	}

}
