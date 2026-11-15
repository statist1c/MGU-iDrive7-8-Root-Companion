// SPDX-License-Identifier: GPL-3.0-only
package dev.mgu.standalone;

import java.net.URI;

/** Pure release policy, shared by updater and offline regression checks. */
final class UpdatePolicy {
    static final String REPOSITORY = "statist1c/MGU-iDrive7-8-Root-Companion";

    static boolean assetUrl(String address) {
        try {
            URI uri = new URI(address);
            return "https".equals(uri.getScheme()) && "github.com".equalsIgnoreCase(uri.getHost())
                    && uri.getUserInfo() == null && uri.getPort() == -1 && uri.getQuery() == null
                    && uri.getFragment() == null && uri.getPath().startsWith("/" + REPOSITORY + "/releases/download/")
                    && !uri.getPath().contains("/../");
        } catch (Exception error) { return false; }
    }

    static boolean eligibleVersion(String tag, String installed, boolean draft, boolean prerelease, boolean previews) {
        try {
            ReleaseVersion version = ReleaseVersion.parse(tag);
            return !draft && (previews || (!prerelease && version.pre.length == 0))
                    && version.compareTo(ReleaseVersion.parse(installed)) > 0;
        } catch (IllegalArgumentException error) { return false; }
    }
}
