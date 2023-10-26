#!/usr/bin/env bash

# List of config dirs containing templates (space delimited)
CONF_DIRS='/usr/local/etc'

# Set our container hostname using this before the instance-id
HOST_PREFIX="${ENVIRONMENT}-andre"

# Command to run after all processing is complete
EXEC_CMD='/usr/local/tomcat/bin/catalina.sh run'

# Swaps environment variables in config file templates with name
# docker-<filename> to <filename> with the subs...in $CONF_DIR
function subst_config() {
  local config_file=$1
  local config_dir=$2
  local target_file=$(echo $config_file | sed "s/docker-//")
  echo "  * subbing ${config_dir}/${config_file}"
  cat ${config_dir}/${config_file} | envsubst > ${config_dir}/${target_file}
}

function subst_configs() {
  local config_dir=$1
  local last_dir=$(pwd)
  echo "*** Substituting docker-* templates in ${config_dir}:"
  cd $config_dir
  for template in docker-*; do
    subst_config $template $config_dir
  done
}

function create_hostname() {
  local instance_id=$(curl -m 1 http://169.254.169.254/latest/meta-data/instance-id)
  local host_prefix=$1
  if [[ $instance_id == "" ]]; then
    export HOSTNAME="${host_prefix}-local"
  else
    instance_id=$(echo ${instance_id} | sed 's/i-//')
    export HOSTNAME="${host_prefix}-${instance_id}"
  fi
  echo "*** Set local HOSTNAME variable to ${HOSTNAME}"
  hostname $HOSTNAME
  echo $HOSTNAME > /etc/hostname
  head -n -1 /etc/hosts > /tmp/hosts.tmp
  tail -n 1 /etc/hosts | sed "s/\$/ ${HOSTNAME}/" >> /tmp/hosts.tmp
  cp /tmp/hosts.tmp /etc/hosts
}

# Substitute our configs
for conf_dir in $CONF_DIRS; do
  subst_configs $conf_dir
done

echo "*** This is build number ${BUILD_NUMBER}"

# Set our container hostname
create_hostname $HOST_PREFIX
export CATALINA_OPTS="${CATALINA_OPTS} -Dinsidr.host=${HOSTNAME}"

# Start up container service
exec $EXEC_CMD
