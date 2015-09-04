# Installation of CARE Plugin #

The CARE plugin can be installed via Eclipse IDE using **Help -> Install New Software** option.

For Eclipse 3.8.x to Eclipse 4.2.x  -> "http://care.googlecode.com/svn/trunk/care-update-site/"

<a href='http://care.googlecode.com/svn/trunk/images/care-install.png'><img src='http://care.googlecode.com/svn/trunk/images/care-install.png' /></a>

## Configuration ##

Some large projects may cause Heap size OutOfMemoryError. In order to avoid the Heap size problem,  update the eclipse.ini file with the following parameters:

```
-vmargs
-Xms512m
-Xmx2048m
-XX:PermSize=256m
```

The eclipse.ini file on **Mac OS X** can be accessed by right-clicking the Eclipse application icon and then clicking **Show Package Contents**. This will open a new Finder window with the Contents folder. In this folder open MacOS folder and the file eclipse.ini exists there.


# Usage Instructions #

## CARE User Interface ##

Once the plugin is installed, it can be accessed through CARE menu (added to the Eclipse workbench) where we can select different refactoring options. Currently, programs can be restructured in two different ways.
  1. Restructure using Move Class Refactoring.
  1. Restructure using Composite Refactoring.
The composite refactoring technique comprises of four types of refactorings including move class, type generalization, introduction of service locator, and static members inlining.

The following figure shows the UI of the CARE plugin. In order to start the refactoring process, programs must be loaded into the Eclipse workspace. To analyse a single project,  click on ![https://sites.google.com/site/careplugin/_/rsrc/1371692455377/documentation/single1.png](https://sites.google.com/site/careplugin/_/rsrc/1371692455377/documentation/single1.png)  button. To analyse all opened projects in the workspace, click on ![https://sites.google.com/site/careplugin/_/rsrc/1371692552262/documentation/multiple1.png](https://sites.google.com/site/careplugin/_/rsrc/1371692552262/documentation/multiple1.png)  button.

<a href='http://care.googlecode.com/svn/trunk/images/care-ui1.png'><img src='http://care.googlecode.com/svn/trunk/images/care-ui1.png?height=178&width=400/></a'>

<h2>CARE Preferences</h2>

We can tweak some options in the CARE plugin. For example, we can edit the maximum number of refactoring steps for a program. The default value is 10. In a similar way, we can tell the plugin about certain classes that should not be moved during the automated refactoring process.<br>
<br>
<a href='https://sites.google.com/site/careplugin/documentation/care-preferences.png'><a href='https://sites.google.com/site/careplugin/documentation/care-preferences.png?height=500&width=560'>https://sites.google.com/site/careplugin/documentation/care-preferences.png?height=500&amp;width=560</a></a>

<h2>Importing Projects</h2>

We have manually configured programs in the Qualitas Corpus as Eclipse projects. Programs are structured as follows:<br>
<br>
<br>
<table border='1' cellspacing='0'>
<tbody>
<tr>
<td><b>Folder<span> </span></b></td>
<td><b>Description</b></td>
</tr>
<tr>
<td>src</td>
<td>Source code contained in .java files goes to this folder.</td>
</tr>
<tr>
<td>bin</td>
<td>The compiled output files (.class) are placed here.</td>
</tr>
<tr>
<td>lib</td>
<td>This folder contains external libraries required by a project. Note that external libraries are excluded from the refactoring process.</td>
</tr>
<tr>
<td>tests</td>
<td>Source code of test cases is placed under this folder, if provided.</td>
</tr>
</tbody>
</table>

Download links for projects:<br>
<br>
<table width='550px' border='1' cellspacing='0'>
<tbody>
<tr>
<blockquote><td width='200px'><b>File Name</b></td>
<td width='150px' align='center'><b>Number of Programs</b></td>
<td width='100px' align='center'><b>File Size</b></td>
<td width='100px' align='center'><b>Download</b></td>
</tr></blockquote>

<tr>
<blockquote><td>sample-programs.zip</td>
<td align='center'>5</td>
<td align='center'>15 MB</td>
<td align='center'><a href='http://db.tt/5cFU8ayV'><img src='http://care.googlecode.com/svn/trunk/images/download.png' height='32px' width='32px' /></a></td>
</tr></blockquote>

<tr>
<blockquote><td>corpus-20101126r.zip</td>
<td align='center'>92</td>
<td align='center'>1 GB</td>
<td align='center'><a href='http://db.tt/w6dcLHbz'><img src='http://care.googlecode.com/svn/trunk/images/download.png' height='32px' width='32px' /></a></td>
</tr>
</tbody>
</table></blockquote>

After downloading the file, use the Import feature in Eclipse IDE to load these programs in a workspace. Before importing projects, the following requirements must be fulfilled:<br>
<ol><li>JDK 1.6.x must be installed on the system. Some projects may fail to compile because they use JDK libraries. This can be verified by accessing Eclipse <b>Preferences</b> and <b>Installed JREs</b>.<br>
</li><li>Under Eclipse Preferences, click on <b>Java -> Compiler -> Errors/Warnings</b>. In the section <b>Deprecated and Restricted APIs</b>, choose Ignore for Forbidden Reference (access rule).</li></ol>

Using the <b>File -> Import</b> feature, projects can be loaded into the workspace, as shown in the following figures:<br>
<br>
<img src='http://care.googlecode.com/svn/trunk/images/import.png' width='564px' />

<img src='http://care.googlecode.com/svn/trunk/images/import1.png' />

After the import process if some projects show compilation errors, select those projects and clean them using <b>Project -> Clean</b> option.<br>
<br>
<h2>Refactoring Output</h2>

After the refactoring process is completed, we can view the refactoring related information in the output folder of each project. The output is generated as comma separated (CSV) files.  The description of the output files for the move class refactoring is as follows:<br>
<br>
<table border='1' cellspacing='0' width='800px'>
<tbody>
<tr>
<td width='25%'><b>File name</b></td>
<td width='75%'><b>Purpose</b></td>
</tr>

<tr>
<td> instances</td>
<td>Keeps track of antipattern instance count decrease after each refactoring.</td>
</tr>

<tr>
<td>metrics</td>
<td>All other metrics are stored in this file, such as strongly connected components metrics.</td>
</tr>
<tr>
<td>g2c-success</td>
<td>Graph-to-Code success rate. This data is used to compute the refactorability. </td>
</tr>
<tr>
<td>constraints</td>
<td>Information related to pre and postconditions is stored here.</td>
</tr>
<tr>
<td>details<span> </span></td>
<td>This file contains detailed information about the removed dependency, such as how many patterns were removed.<span> </span></td>
</tr>
<tr>
<td>error-edge</td>
<td>Due to limitations of Eclipse refactorings, some refactorings may not be rolled back properly, thus failing the compilation. This file contains the name of class that created the problem. We can put this class name in the list of blacklisted classes in Eclipse Preferences.</td>
</tr>
<tr>
<td>package-metrics</td>
<td>Here all the package related metrics are storedincluding<span>before and after values.</span></td>
</tr>

</tbody>
</table>

The output files for the composite refactoring are generated under the folder named <b>output-composite-refactoring</b>. Here, a sub-folder <b>refactored-code</b> exists, which contains source code before and after refactoring for each successful refactoring.