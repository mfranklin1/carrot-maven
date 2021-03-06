/**
 * Copyright (C) 2010-2012 Andrei Pozolotin <Andrei.Pozolotin@gmail.com>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.carrotgarden.maven.scr;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

/**
 * generate component descriptors form annotated java classes
 * 
 * @goal generate
 * 
 * @phase prepare-package
 * 
 * @inheritByDefault true
 * 
 * @requiresDependencyResolution test
 * 
 */
public class CarrotOsgiScrGenerate extends CarrotOsgiScr {

	private int descriptorCounter;
	private int allclassesCounter;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void execute() throws MojoFailureException {

		if (isImproperPackaging()) {
			return;
		}

		getLog().info("");
		getLog().info("excludedServices");
		for (final String service : excludedServices) {
			getLog().info("\t service=" + service);
		}

		getLog().info("");
		getLog().info("improperPackaging");
		for (final String packaging : improperPackaging) {
			getLog().info("\t packaging=" + packaging);
		}

		//

		descriptorCounter = 0;
		allclassesCounter = 0;

		final long timeStart = System.nanoTime();

		if (!isProcessMainClasses && !isProcessTestClasses) {
			getLog().warn("you have not selected neither main nor test classes");
		}

		if (isProcessMainClasses) {
			processClassesDirectory(ClassesSelector.COMPILE);
		}

		if (isProcessTestClasses) {
			processClassesDirectory(ClassesSelector.TESTING);
		}

		if (isIncludeEmptyDescriptor) {
			includeEmptyDescriptor();
		}

		if (isIncludeGeneratedDescritors) {
			includeDescriptorResource();
		}

		final long timeFinish = System.nanoTime();

		//

		getLog().info("");

		final long timeDiff = timeFinish - timeStart;
		final long timeRate = descriptorCounter == 0 ? 0 : timeDiff
				/ descriptorCounter;
		getLog().info("combined classes count = " + allclassesCounter);
		getLog().info("descriptor class count = " + descriptorCounter);
		getLog().info("time, millis total     = " + timeDiff / 1000000);
		getLog().info("rate, millis per descr = " + timeRate / 1000000);

	}

	protected void processClassesDirectory(final ClassesSelector selector)
			throws MojoFailureException {

		try {

			final File classesDirectory = selector.getClassesDirectory(this);

			if (isValidDirectory(classesDirectory)) {
				getLog().info("");
				getLog().info("input classes = " + classesDirectory);
			} else {
				throw new MojoFailureException("classes directory invalid");
			}

			/** collect all class files */
			@SuppressWarnings("unchecked")
			final Iterator<File> iter = FileUtils.iterateFiles(
					classesDirectory, EXTENSIONS, IS_RECURSIVE);

			final ClassLoader loader = getClassloader(selector);

			getLog().info("");
			getLog().info("output directory = " + outputDirectorySCR());

			while (iter.hasNext()) {

				/** discovered *.class file */
				final File file = iter.next();

				/** resolved class name */
				final String name = getClassName(classesDirectory, file);

				getLog().debug("\t class : " + name);

				/** make individual descriptor */
				final String text = getMaker().make(loader, name);

				/** non component returns null */
				final boolean isComponent = text != null;

				allclassesCounter++;

				if (isComponent) {

					final String outputFile = outputFileSCR(name);

					getLog().info("\t descriptor = " + outputFile);

					saveDescriptor(name, text);

					descriptorCounter++;

				} else {

					getLog().debug("\t class is not a component");

				}

			}

			getLog().info("");

			if (descriptorCounter == 0) {
				getLog().warn("did not find any active scr components");
			} else {
				getLog().info("active components count = " + descriptorCounter);
			}

		} catch (final MojoFailureException exception) {

			throw exception;

		} catch (final Throwable exception) {

			throw new MojoFailureException("execution failure", exception);

		}

	}

	/**
	 * 
	 * descriptor file name convention:
	 * 
	 * from: com.carrotgarden.test.TestComp
	 * 
	 * into: com.carrotgarden.test.TestComp.xml
	 * 
	 */
	protected void saveDescriptor(final String name, final String text)
			throws Exception {

		final File file = new File(outputDirectorySCR(), outputFileSCR(name));

		FileUtils.writeStringToFile(file, text);

	}

	protected String outputFileSCR(final String name) {
		return name + "." + outputExtensionSCR;
	}

	/**
	 * @return java class FQN
	 */
	protected String getClassName(final File classesDirectory,
			final File classFile) {

		final URI folderURI = classesDirectory.toURI();
		final URI fileURI = classFile.toURI();

		final String path = folderURI.relativize(fileURI).getPath();

		/**
		 * cut out file extension and convert to java class FQN
		 * 
		 * from: com/carrotgarden/test/TestComp.class
		 * 
		 * into: com.carrotgarden.test.TestComp
		 * 
		 */

		final int index = path.lastIndexOf(".");

		final String name = path.substring(0, index).replace("/", ".");

		return name;

	}

	/**
	 * @return class loader that will include both project and plugin
	 *         dependencies
	 **/
	protected ClassLoader getClassloader(final ClassesSelector selector)
			throws Exception {

		final List<String> pathList = selector.getClasspathElements(project);

		final URL[] entryUrlArray = new URL[pathList.size()];

		int index = 0;
		for (final String path : pathList) {
			final URL entryURL = new File(path).toURI().toURL();
			getLog().info("\t dependency = " + entryURL);
			entryUrlArray[index++] = entryURL;
		}

		/** maven plugin class loader */
		final ClassLoader TCCL = Thread.currentThread().getContextClassLoader();

		/** class path loader for a selector */
		final URLClassLoader loader = new URLClassLoader(entryUrlArray, TCCL);

		return loader;

	}

	protected enum ClassesSelector {

		COMPILE() {

			@Override
			public List<String> getClasspathElements(final MavenProject project)
					throws DependencyResolutionRequiredException {
				return project.getCompileClasspathElements();
			}

			@Override
			public File getClassesDirectory(final CarrotOsgiScrGenerate mojo) {
				return mojo.outputMainClasses;
			}

		},

		TESTING() {

			@Override
			public List<String> getClasspathElements(final MavenProject project)
					throws DependencyResolutionRequiredException {
				return project.getTestClasspathElements();
			}

			@Override
			public File getClassesDirectory(final CarrotOsgiScrGenerate mojo) {
				return mojo.outputTestClasses;
			}

		},

		;

		public abstract List<String> getClasspathElements(MavenProject project)
				throws DependencyResolutionRequiredException;

		public abstract File getClassesDirectory(CarrotOsgiScrGenerate mojo);

	}

	protected void includeDescriptorResource() {

		final Resource resource = new Resource();

		final String sourcePath = outputDirectorySCR().getPath();
		final String targetPath = targetDirectorySCR;

		resource.setDirectory(sourcePath);
		resource.setTargetPath(targetPath);

		getLog().info("");
		getLog().info("including descriptor resource = " + resource);

		project.addResource(resource);

	}

	protected static final String NULL_XML = "null.xml";

	protected void includeEmptyDescriptor() throws MojoFailureException {

		final URL source = getClass().getResource(NULL_XML);

		final File target = new File(outputDirectorySCR(), NULL_XML);

		try {
			FileUtils.copyURLToFile(source, target);
		} catch (final Exception e) {
			throw new MojoFailureException("can not get " + NULL_XML, e);
		}

		getLog().info("");
		getLog().info("including empty descriptor = " + target);

	}

}
