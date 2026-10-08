---
layout: page
title: Setting up and getting started
---

* Table of Contents
{:toc}


--------------------------------------------------------------------------------------------------------------------

## Setting up the project on your computer

<div markdown="span" class="alert alert-warning">:exclamation: **Caution:**

Follow the steps below precisely. The setup may fail if you skip or change a step.
</div>

First, **fork** this repo, and **clone** the fork into your computer.

Use **JDK 25** for development and Java 25 when launching the packaged JAR. On macOS, follow the [course's Mac advisory](https://nus-cs2103-ay2627-s1.github.io/website/admin/programmingLanguages.html) and install **Azul JDK 25 with JavaFX (`25.0.3.fx-zulu`)** using the [Mac installation guide](https://se-education.org/guides/tutorials/javaInstallationMac.html). This is the required macOS setup for development and release checks. Confirm `java -version` reports Java 25 in the terminal used for Gradle and JAR launches; if using SDKMAN, `sdk current java` should identify `25.0.3.fx-zulu` on macOS.

If you plan to use IntelliJ IDEA (highly recommended):
1. **Configure the JDK**: Follow the guide [_[se-edu/guides] IDEA: Configuring the JDK_](https://se-education.org/guides/tutorials/intellijJdk.html) to ensure IntelliJ's project SDK and Gradle JVM use **JDK 25**, including the prescribed Azul JDK with JavaFX on macOS.
1. **Import the project as a Gradle project**: Follow the guide [_[se-edu/guides] IDEA: Importing a Gradle project_](https://se-education.org/guides/tutorials/intellijImportGradleProject.html) to import the project into IDEA.<br>
  :exclamation: Note: Importing a Gradle project is slightly different from importing a normal Java project.
1. **Verify the setup**:
   1. Run `seedu.address.Main` and try a few commands.
   1. [Run the tests](Testing.html) to ensure they all pass.

--------------------------------------------------------------------------------------------------------------------

## Before writing code

1. **Configure the coding style**

   If using IDEA, follow the guide [_[se-edu/guides] IDEA: Configuring the code style_](https://se-education.org/guides/tutorials/intellijCodeStyle.html) to set up IDEA's coding style to match ours.

   <div markdown="span" class="alert alert-primary">:bulb: **Tip:**

   Optionally, follow the guide [_[se-edu/guides] Using Checkstyle_](https://se-education.org/guides/tutorials/checkstyle.html) to learn how to use Checkstyle in IDEA, for example to report problems _as_ you write code.
   </div>

1. **Set up CI**

   This project includes GitHub Actions configuration files in `.github/workflows`. GitHub runs CI automatically for every push and pull request. No setup is required.

1. **Learn the design**

   When you are ready to start coding, we recommend that you get some sense of the overall design by reading about [AddressBook’s architecture](DeveloperGuide.html#architecture).

1. **Do the tutorials**
   These tutorials will help you get acquainted with the codebase.

   * [Tracing code](https://se-education.org/guides/tutorials/ab3TracingCode.html)
   * [Adding a new command](https://se-education.org/guides/tutorials/ab3AddRemark.html)
   * [Removing fields](https://se-education.org/guides/tutorials/ab3RemovingFields.html)
