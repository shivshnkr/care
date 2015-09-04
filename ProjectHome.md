# Code and Architecture Refactoring Environment (CARE) #
## Automation of Dependency-Breaking Refactorings in Java ##

Over a period of time software systems grow large and become complex due to unsystematic changes that create a high level of interconnection among software artefacts. Consequently, maintenance becomes expensive and even making small changes may require considerable resources due to change propagation in the system, a phenomenon known as ripple effects.

Industrial evidence suggests that more resources are spent on the maintenance phase than on the initial development. It is evident that companies make huge investments to maintain legacy systems until a point comes where a complete restructuring of the system is required. In most cases, it becomes very expensive to refurbish legacy systems manually due to their inherent complexity. Several semi-automated solutions have been proposed to restructure simplified models of existing systems. It is still expensive, in terms of resources, to translate those model level transformations into source code transformations or refactorings. The question that arises here is whether we can automate the application of model level changes on the source code of programs.

In this project, we have developed novel algorithms to automate the application of a class of architectural transformations related to improving modularity of existing programs. In order to evaluate our approach, we have analysed a large dataset of open source programs to determine whether the manipulation of models can be translated into source code refactorings, whether we can define constraints on those refactorings to preserve program correctness, and to which extent the automation of the whole process is possible. The results indicate that this automation process can be achieved to a significant level, which implies that certain economic benefits can be gained from the process.

# Project at a Glance #

The CARE-Plugin project aims to develop Eclipse refactoring plugins that facilitate in achieving software modularity. The CARE plugin deals with four types of antipatterns: **circular dependencies between packages** (SCD), **subtype knowledge** (STK), **abstraction without decoupling** (AWD), and **degenerated inheritance** (DEGINH).  Some of the features of the plugin are as follows:
  1. The plugin automatically completes the refactoring cycle, i.e., identifies refactoring opportunities, checks preconditions, performs refactorings, and checks post conditions with the push of a button.
  1. Several features can be customized.
  1. Output files are generated to review the refactoring process.
  1. Programs can  be analysed in a batch bode or as a single project.

# The Refactoring Process #
An overview of the refactoring process is given below:
  1. The dependency graph of a program is built from the bytecode
  1. Antipattern instances are computed for SCD, STK, AWD and DEGINH antipatterns.
  1. Critical dependencies that cause the creation of antipatterns are identified
  1. The source code of the program is parsed into Abstract syntax tree (AST) for manipulation
  1. Preconditions are checked to determine whether a dependency can be removed or not.
  1. If the preconditions are satisfied, apply the refactoring on the program’s ASTs. Otherwise try the next high-scored edge.
  1. Evaluate postconditions to check whether the applied refactoring has introduced any errors.
  1. If postconditions are satisfied, update the program’s source code. Otherwise, rollback the ASTs to their previous states.
  1. Repeat the process until all antipatterns instances are removed or a certain number of iterations are performed (The default MAX value is 10).

![http://care.googlecode.com/svn/trunk/images/refac_process.jpg](http://care.googlecode.com/svn/trunk/images/refac_process.jpg)