package de.unicate.serviceexamples.services;

/**
 * The RemoteService is just a wrapper class for the BaseService, to
 * enable the "remoteness" in the manifest (process attribute in Manifest.xml)
 */
public class RemoteService extends BaseService {
    @Override
    protected String getTag() {
        return RemoteService.class.getSimpleName();
    }
}
