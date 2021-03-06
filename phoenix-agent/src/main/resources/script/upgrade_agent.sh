
set -e
set -u

log "PID is $$"
log "CMD is $0 $@"

AGENT_CLASS="com.dianping.phoenix.agent.PhoenixAgent"
AGENT_DRY_RUN_CLASS="com.dianping.phoenix.agent.PhoenixAgentDryRun"
DRY_RUN_PORT=3474

function ensure_agent_started {
	log "checking whether agent process alive"
	agent_process_num=`jps -lvm | awk -v javaclass=$AGENT_CLASS '$2==javaclass{print $0}' | wc -l`
	if [ $agent_process_num -eq 0 ];then
		log_error "no agent process found, try to git reset agent dir and start it"
		cd $agent_dir
		git reset --hard
		chmod +x $agent_dir/startup.sh
		$agent_dir/startup.sh
		cd - > /dev/null
	fi
}
trap ensure_agent_started EXIT

while getopts "g:v:t:a:l:" option;do
	case $option in
			g)      agent_git_url=$OPTARG;;
			v)      agent_version=$OPTARG;;
			t)      agent_war_tmp=$OPTARG;;
			a)      agent_dir=$OPTARG;;
			l)      tx_log_file=$OPTARG;;
	esac
done

sleep 1
create_git_repo $agent_dir
sync_git_repo $agent_git_url $agent_version $agent_war_tmp
kill_by_javaclass $AGENT_DRY_RUN_CLASS
new_agent_ok=true
chmod +x $agent_war_tmp/startup.sh
$agent_war_tmp/startup.sh $AGENT_DRY_RUN_CLASS $DRY_RUN_PORT || { new_agent_ok=false; }
if [ $new_agent_ok == true ];then
	log "new agent is valid, replace current agent"
	if [ -f $tx_log_file ];then
		exec 1>>$tx_log_file 2>&1
	fi
	kill_by_javaclass $AGENT_CLASS
	upgrade_local_git_repo $agent_war_tmp $agent_dir
	chmod +x $agent_dir/startup.sh
	$agent_dir/startup.sh
else
	log_error "new agent is corrputed, won't update"	
fi
