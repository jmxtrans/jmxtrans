Name: jmxtrans
Version: %{VERSION}
Release: %{RELEASE}
Summary: JMX Transformer - more than meets the eye
Group: Applications/Communications
URL: http://http://code.google.com/p/jmxtrans//
Vendor: Jon Stevens
Packager: Henri Gomez <henri.gomez@gmail.com>
License: OpenSource Software by Jon Stevens
BuildArch:  noarch

Source0: %{JMXTRANS_SOURCE}
Source1: jmxtrans.init

BuildRoot: %{_tmppath}/build-%{name}-%{version}-%{release}

Requires(pre):   /usr/sbin/groupadd
Requires(pre):   /usr/sbin/useradd

%define xuser       jmxtrans
%define xapp        jmxtrans

%define xappdir         %{_usr}/share/%{xapp}
%define xlibdir         %{_var}/lib/%{xapp}
%define xlogdir         %{_var}/log/%{xapp}
%define xconf           %{_sysconfdir}/sysconfig/%{xapp}

%description
jmxtrans is very powerful tool which reads json configuration files of servers/ports and jmx domains - attributes - types.
Then outputs the data in whatever format you want via special 'Writer' objects which you can code up yourself.
It does this with a very efficient engine design that will scale to querying thousands of machines.

%prep
%setup -q -n %{xapp}-%{version}

%build

%install
# Prep the install location.
rm -rf $RPM_BUILD_ROOT
mkdir -p $RPM_BUILD_ROOT%{xappdir}
mkdir -p $RPM_BUILD_ROOT%{xlibdir}
mkdir -p $RPM_BUILD_ROOT%{xlogdir}
mkdir -p $RPM_BUILD_ROOT%{_sysconfdir}/init.d
mkdir -p $RPM_BUILD_ROOT%{_sysconfdir}/sysconfig

# remove source (unneeded here)
rm -rf src/com
cp -rf * $RPM_BUILD_ROOT%{xappdir}
cp  %{SOURCE1} $RPM_BUILD_ROOT%{_sysconfdir}/init.d/%{xapp}

# ensure shell scripts are executable
chmod 755 $RPM_BUILD_ROOT%{xappdir}/*.sh

%clean
rm -rf $RPM_BUILD_ROOT

%pre
/usr/sbin/useradd -c "JMXTrans" \
        -s /sbin/nologin -r -d %{xappdir} %{xuser} 2> /dev/null || :

%post
if [ $1 = 1 ]; then
  /sbin/chkconfig --add %{xapp}

  # get number of cores so we can set number of GC threads
  CPU_CORES=$(cat /proc/cpuinfo | grep processor | wc -l)
  NEW_RATIO=8

  # defaults for JVM
  HEAP_SIZE="512"
  HEAP_NUMBER=$(echo $HEAP_SIZE|sed 's/[a-zA-Z]//g')
  NEW_SIZE=$(expr $HEAP_SIZE / $NEW_RATIO)

  # populate sysconf file
  echo "# configuration file for package %{xapp}" > %{xconf}
  echo "export JAR_FILE=\"/usr/share/jmxtrans/jmxtrans-all.jar\"" >> %{xconf}
  echo "export LOG_DIR=\"/var/log/%{xapp}\"" >> %{xconf}
  echo "export SECONDS_BETWEEN_RUNS=60" >> %{xconf}
  echo "export JSON_DIR=\"%{xlibdir}\"" >> %{xconf}
  echo "export HEAP_SIZE=${HEAP_SIZE}" >> %{xconf}
  echo "export NEW_SIZE=${NEW_SIZE}" >> %{xconf}
  echo "export CPU_CORES=${CPU_CORES}" >> %{xconf}
  echo "export NEW_RATIO=${NEW_RATIO}" >> %{xconf}
  echo "export JMXTRANS_LOG_LEVEL=debug" >> %{xconf}

fi

%preun
if [ $1 = 0 ]; then
  /sbin/service %{xapp} stop > /dev/null 2>&1
  /sbin/chkconfig --del %{xapp}
  rm -f  %{xconf}
fi

%posttrans
/sbin/service %{xapp} condrestart >/dev/null 2>&1 || :


%files
%defattr(-,root,root)
%attr(0755, root, root)        %{_sysconfdir}/init.d/%{xapp}
#%config(noreplace)             %{_sysconfdir}/sysconfig/%{xapp}
%config(noreplace)             %{xlibdir}
%attr(0755,%{xuser}, %{xuser}) %{xlogdir}
%{xappdir}/*
%doc %{xappdir}/README.html


%changelog
* Wed Jul 19 2011 Henri Gomez <henri.gomez@gmail.com> - 223-1
- Initial RPM package to be used and build with ci systems.
