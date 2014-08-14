package de.unicate.serviceexamples.callback;

import de.unicate.serviceexamples.callback.IServiceCallback;

interface IService {
	int getPID();
	long getTID();
	void registerCallback(IServiceCallback callback);
}