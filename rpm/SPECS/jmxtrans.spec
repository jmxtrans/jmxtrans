# Avoid unnecessary debug-information (native code)
%define		debug_package %{nil}

# Avoid CentOS 5/6 extras processes on contents (especially brp-java-repack-jars)
%define __os_install_post %{nil}

Name: jmxtrans
Version: %{VERSION}
Release: %{RELEASE}
Summary: JMX Transformer - more than meets the eye
Group: Applications/Communications
URL: https://github.com/jmxtrans/jmxtrans/
Vendor: Jon Stevens
Packager: Henri Gomez <henri.gomez@gmail.com>
License: OpenSource Software by Jon Stevens
BuildArch:  noarch

Source0: %{JMXTRANS_SOURCE}
Source1: jmxtrans.init
Source2: systemd

BuildRoot: %{_tmppath}/build-%{name}-%{version}-%{release}

Requires(pre):   /usr/sbin/groupadd
Requires(pre):   /usr/sbin/useradd

%define xuser       jmxtrans

%define xappdir         %{_usr}/share/jmxtrans
%define xlibdir         %{_var}/lib/jmxtrans
%define xlogdir         %{_var}/log/jmxtrans
%define xconf           %{_sysconfdir}/sysconfig/jmxtrans

%define _systemdir        /lib/systemd/system
%define _initrddir        %{_sysconfdir}/init.d

%description
jmxtrans is very powerful tool which reads json configuration files of servers/ports and jmx domains - attributes - types.
Then outputs the data in whatever format you want via special 'Writer' objects which you can code up yourself.
It does this with a very efficient engine design that will scale to querying thousands of machines.

%prep
%setup -q -n jmxtrans-%{version}

%build

%install
# Prep the install location.
rm -rf   %{buildroot}
mkdir -p %{buildroot}%{_bindir}
mkdir -p %{buildroot}%{xappdir}
mkdir -p %{buildroot}%{xlibdir}
mkdir -p %{buildroot}%{xlogdir}
mkdir -p %{buildroot}%{_initrddir}
mkdir -p %{buildroot}%{_sysconfdir}/sysconfig
mkdir -p %{buildroot}%{_systemdir}

# remove source (unneeded here)
rm -rf src/com
cp -rf * %{buildroot}%{xappdir}
cp  %{SOURCE1} %{buildroot}%{_initrddir}/jmxtrans

# copy yaml2jmxtrans.py to bin
cp tools/yaml2jmxtrans.py %{buildroot}%{_bindir}
chmod 755 %{buildroot}%{_bindir}/yaml2jmxtrans.py

# copy doc (if existing)
cp -rf doc %{buildroot}%{xappdir}

# Setup Systemd
cp %{SOURCE2} %{buildroot}%{_systemdir}/jmxtrans.service

# ensure shell scripts are executable
chmod 755 %{buildroot}%{xappdir}/*.sh

%clean
rm -rf %{buildroot}

%pre
%if 0%{?suse_version} > 1140
%service_add_pre jmxtrans.service
%endif
USER_ID=`id -u %{xuser} 2>/dev/null`
if [ -z "$USER_ID" ]; then
  /usr/sbin/useradd -c "${project.name}" -s /bin/sh -r -d \
									    ${package.install.dir} -U %{xuser} 2> /dev/null || :
fi

%post
%if 0%{?suse_version} > 1140
%service_add_post jmxtrans.service
%endif
if [ $1 = 1 ]; then
  /sbin/chkconfig --add jmxtrans

  # get number of cores so we can set number of GC threads
  CPU_CORES=$(cat /proc/cpuinfo | grep processor | wc -l)
  NEW_RATIO=8

  # defaults for JVM
  HEAP_SIZE="512"
  HEAP_NUMBER=$(echo $HEAP_SIZE|sed 's/[a-zA-Z]//g')
  NEW_SIZE=$(expr $HEAP_SIZE / $NEW_RATIO)

  # populate sysconf file
  echo "# configuration file for package jmxtrans" > %{xconf}
  echo "export JAR_FILE=\"/usr/share/jmxtrans/jmxtrans-all.jar\"" >> %{xconf}
  echo "export LOG_DIR=\"/var/log/jmxtrans\"" >> %{xconf}
  echo "export SECONDS_BETWEEN_RUNS=60" >> %{xconf}
  echo "export JSON_DIR=\"%{xlibdir}\"" >> %{xconf}
  echo "export HEAP_SIZE=${HEAP_SIZE}" >> %{xconf}
  echo "export NEW_SIZE=${NEW_SIZE}" >> %{xconf}
  echo "export CPU_CORES=${CPU_CORES}" >> %{xconf}
  echo "export NEW_RATIO=${NEW_RATIO}" >> %{xconf}
  echo "export LOG_LEVEL=debug" >> %{xconf}
  echo "export PIDFILE=/var/run/jmxtrans.pid" >> %{xconf}

fi

%preun
%if 0%{?suse_version} > 1140
%service_del_preun jmxtrans.service
%endif
if [ $1 = 0 ]; then
  /sbin/service jmxtrans stop > /dev/null 2>&1
  /sbin/chkconfig --del jmxtrans
  /usr/sbin/userdel %{xuser} 2> /dev/null
  rm -f  %{xconf}
fi

%posttrans
/sbin/service jmxtrans condrestart >/dev/null 2>&1 || :

%postun
%if 0%{?suse_version} > 1140
%service_del_postun jmxtrans.service
%endif


%files
%defattr(-,root,root)
%{_bindir}/*
%attr(0755, root,root)  %{_initrddir}/jmxtrans
%attr(0644,root,root)   %{_systemdir}/jmxtrans.service
#%config(noreplace)     %{_sysconfdir}/sysconfig/jmxtrans
%config(noreplace)      %{xlibdir}
%attr(0755,%{xuser}, %{xuser}) %{xlogdir}
%{xappdir}/*
%doc %{xappdir}/README.html
%doc %{xappdir}/doc


%changelog
* Sat Mar 23 2013 Henri Gomez <henri.gomez@gmail.com> - 242-1
- jmxtrans v242 introduce a Graphite pool to keep connections low

* Thu Mar 21 2013 Henri Gomez <henri.gomez@gmail.com> - 241-2
- Added yaml2jmxtrans.py and docs

* Thu Mar 21 2013 Henri Gomez <henri.gomez@gmail.com> - 241-1
- Fixed LocalMBean Server issue.

* Wed Jul 19 2011 Henri Gomez <henri.gomez@gmail.com> - 223-1
- Initial RPM package to be used and build with ci systems.
