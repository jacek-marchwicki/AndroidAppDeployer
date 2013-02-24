# AppDeployer
This project aims to create a sample application for synchronization over REST.

## License
Copyright (C) 2013 Jacek Marchwicki &lt;jacek.marchwicki@gmail.com&gt;

Licensed under the Apache License, Verision 2.0

## Running

### Before start
if you have a problem with certificate add *GIT\_SSL\_NO\_VERIFY=true* before git submodule update line

	GIT_SSL_NO_VERIFY=true  git submodule update

Im sorry about this certificate issue, but never is enough time to fix problems like those

**If (only if) you have ssh key in appunite review system** you should setup global alias in *~/.gitconfig* file.

	git config --global url.ssh://review.appunite.com.insteadOf https://review.appunite.com

### Go to the work
downloading source code 

	git clone <repo> AndroidAppDeployer
	cd AndroidAppDeployer
	git submodule init
	git submodule sync #if you are updating source code
	git submodule update

go to eclipse and add *AndroidAppDeployer* project and all sub-projects (those listed in *AndroidAppDeployer/project.properties*)
run and have fun.

