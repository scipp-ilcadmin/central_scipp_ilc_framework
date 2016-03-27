frameworkdir=$HOME/ilc_primary/central_scipp_ilc_framework
userfile=$HOME/admin_scripts/scipp_ilc_users_list.txt

cd $frameworkdir/src/driver_collective
for user in $(cat $userfile); do
    if [[ ! -d $user ]]; then mkdir $user; fi;
    cp -r /local-data/$user/scipp_ilc_framework/src/driver/ $user
    git add $user/*java
done;

cd $frameworkdir;
git commit -am "autocommit on $(date)";
