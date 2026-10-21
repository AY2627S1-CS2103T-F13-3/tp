package ponhub.model;

/**
 * Stages a complete canonical aggregate and its people filter on the application's model thread.
 * Failed commands discard this object without touching the live observable list or its selection.
 */
public final class CanonicalTransaction implements ModelTransaction<PonHubData> {
    private final PonHubData live;
    private final PeopleView liveView;
    private final PonHubDataState before;
    private final PonHubData staged;
    private final PeopleView stagedView;

    CanonicalTransaction(PonHubData live, PeopleView liveView) {
        this.live = live;
        this.liveView = liveView;
        before = live.exportState();
        staged = new PonHubData(before);
        stagedView = new PeopleView(staged);
        stagedView.copyFilterFrom(liveView);
    }

    @Override
    public PonHubData getStagedModel() {
        return staged;
    }

    /**
     * Returns the staged view used to resolve command indices and prepare view changes.
     * Refresh it after staged mutations if the command needs to query the updated projection.
     */
    public PeopleView getStagedPeopleView() {
        return stagedView;
    }

    @Override
    public boolean hasOperationalChanges() {
        return !before.equals(staged.exportState());
    }

    /**
     * Publishes staged data and filters in memory without validation or I/O.
     * The command executor must save any changed operational snapshot successfully before calling this method.
     */
    @Override
    public void commit() {
        boolean dataChanged = hasOperationalChanges();
        boolean filterChanged = !liveView.getRoleFilter().equals(stagedView.getRoleFilter())
                || !liveView.getSearchCriteria().equals(stagedView.getSearchCriteria());
        if (dataChanged) {
            live.resetData(staged.exportState());
        }
        if (dataChanged || filterChanged) {
            liveView.copyFilterFrom(stagedView);
        }
    }
}
