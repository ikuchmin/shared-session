package ru.udya.sharedsession.domain

class SharedUserSessionImpl implements SharedUserSession {

    protected String sharedId

    SharedUserSessionImpl(String sharedId) {
        this.sharedId = sharedId
    }

    @Override
    Serializable getSharedId() {
        return this.sharedId
    }
}
