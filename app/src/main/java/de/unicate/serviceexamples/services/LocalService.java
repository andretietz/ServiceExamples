package de.unicate.serviceexamples.services;

/**
 * The LocalService is just a wrapper class for the BaseService, to
 * enable the "localness" in the manifest (without the process attribute in xml)
 */
public class LocalService extends BaseService {
    @Override
    protected String getTag() {
        return LocalService.class.getSimpleName();
    }
}
