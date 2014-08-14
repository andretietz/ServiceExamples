package de.unicate.serviceexamples.callback;

// Declare any non-default types here with import statements

interface IServiceCallback {
	oneway void receiveInformation(long time);
}
