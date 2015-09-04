#Brief documentation on how to use the CARE plugins.

# Introduction #
Brief documentation on how to use the CARE plugins.

# Details #

Use the following steps to build the CARE plugins to your workspace.
  * Download the required plugin source code (currently move refactoring is supported)
  * Import the project into Eclipse workspace or checkout using SVN client from https://care.googlecode.com/svn/trunk.
  * Create a new Eclipse Application configuration. Set the VM Arguments to -Xmx 1024m, -XX:PermSize=156m
  * The entry point to the refactoring process is **nz.ac.massey.cs.care.move.views.CareView** . In this class setup the absolute workspace path of the CARE plugin project. This is where all output files are written. The variable to update is called **WORKSPACE\_PATH**
  * Once you run the project as Eclipse Application, you should see the CARE menu in the target eclipse application. Click on the Move Refactoring and you'll be able to see a pane at the bottom.
  * Now import a test project into the target eclipse workspace.
  * In the original workspace, under the CARE project, there is a folder called **projects-todo**. In that folder create an empty file with the same name as that of the project you are going to refactor.
  * You can run the refactoring process using a + on the Move refactoring pane in the target eclipse workspace. Multiple projects can also be analyzed.

For any questions please contact alishah\_ph at yahoo.com