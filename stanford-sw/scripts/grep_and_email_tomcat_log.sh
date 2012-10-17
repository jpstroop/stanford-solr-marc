#! /bin/bash
grep 'ERROR\|WARN\|FATAL' /var/log/tomcat6/catalina.out | mail -s 'solr log warning, error and fatal messages' (email address here)
