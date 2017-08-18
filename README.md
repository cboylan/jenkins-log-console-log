# Console Log to Workspace

This is a Jenkins plug-in that allows you to save the console log to a file in the
build's workspace for further processing.

One of its uses is to allow post-processing tools such as log analyzers that
only reads files to do its work.

It is available as a build-step and as a post-build action.

To build the plugin, install maven and jdk (java-development-kit) and run:

    mvn package

or

    mvn install

The built plugin-binary becomes available as `target/console-log-to-workspace.hpi`.

## Tribute

This plugin is based on the work of (forked from) Clark Boylan's "Jenkins Console Log To Workspace Plugin".
His version is no longer maintained and this version strive at being the "official" version from now on (August, 2017).
