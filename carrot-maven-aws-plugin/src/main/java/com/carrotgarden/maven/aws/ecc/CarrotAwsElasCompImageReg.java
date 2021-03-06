/**
 * Copyright (C) 2010-2012 Andrei Pozolotin <Andrei.Pozolotin@gmail.com>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.carrotgarden.maven.aws.ecc;

import java.io.File;
import java.util.Properties;

import org.apache.maven.plugin.MojoFailureException;

import com.amazonaws.services.ec2.model.Image;
import com.carrotgarden.maven.aws.ecc.ElasticCompute.State;

/**
 * register/create new ami image from existing AWS ElasticCompute instance
 * 
 * @goal elastic-compute-image-register
 * 
 * @phase prepare-package
 * 
 * @inheritByDefault true
 * 
 * @requiresDependencyResolution test
 * 
 */
public class CarrotAwsElasCompImageReg extends CarrotAwsElasComp {

	/**
	 * AWS ElasticCompute <b>instance id</b> to use for image create; if
	 * provided explicitly in pom.xml, then it will override value form
	 * properties file; this value must come either from image properties file
	 * of from pom.xml properties
	 * 
	 * @parameter
	 */
	protected String imageInstanceId;

	/**
	 * AWS ElasticCompute ami image name; must be unique under your aws account;
	 * 
	 * @required
	 * @parameter default-value="amazon-image-name"
	 */
	protected String imageName;

	/**
	 * AWS ElasticCompute ami image description
	 * 
	 * @required
	 * @parameter default-value="amazon-image-description"
	 */
	protected String imageDescription;

	/**
	 * AWS ElasticCompute image create input properties file; normally created
	 * by previous cloud formation invocation; must include InstanceId property;
	 * 
	 * @required
	 * @parameter default-value="./target/formation/formation-output.properties"
	 */
	protected File imagePropertiesInputFile;

	/**
	 * name of the property in input properties file that will be used to
	 * discover <b>InstanceId</b> to use as basis for image create operation
	 * 
	 * @required
	 * @parameter default-value="InstanceId"
	 */
	protected String imagePropertyInstanceId;

	@Override
	public void execute() throws MojoFailureException {

		try {

			getLog().info("image reg init [" + imageName + "]");

			final Properties props = load(imagePropertiesInputFile);

			final String instanceId;
			if (imageInstanceId == null) {
				instanceId = props.getProperty(imagePropertyInstanceId);
			} else {
				instanceId = imageInstanceId;
			}

			final ElasticCompute compute = getElasticCompute();

			final Image image = compute.imageRegister(instanceId, imageName,
					imageDescription);

			final State state = State.fromValue(image.getState());

			switch (state) {
			case AVAILABLE:
				break;
			default:
				throw new IllegalStateException("image reg failed : \n" + image);
			}

			getLog().info("image reg done [" + imageName + "]");

		} catch (final Exception e) {

			throw new MojoFailureException("bada-boom", e);

		}

	}

}
