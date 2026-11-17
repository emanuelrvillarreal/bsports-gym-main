package com.blue.open.Core.Configuration;

import com.blue.open.Core.CoreVersion;
import com.blue.open.Version;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

@Component
public class VersionInfoContributor implements InfoContributor {

    @Override
    public void contribute(Info.Builder builder) {
        builder.withDetail("version", Version.VERSION);
        builder.withDetail("coreVersion", CoreVersion.CORE_VERSION);
    }
}
