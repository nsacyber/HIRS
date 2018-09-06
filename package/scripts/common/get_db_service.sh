if [[ $(rpm -qa mariadb*) ]]; then
  echo "mariadb"
elif [[ $(rpm -qa mysql-server*) ]]; then
  echo "mysqld"
else
  echo "Could not determine installed database"
  exit 1
fi