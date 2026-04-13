package com.orodent.tonv2.app.navigation;

public interface LaboratoryNavigator {
    void showLaboratory();
    void showCreateComposition();
    void showCreateComposition(int productId);
    void showCreateDiskModel();
    void showCreateDiskModel(int blankModelId);
    void showLaboratoryCompositionArchive();
    void showLaboratoryDiskModelArchive();
    void showItemSetup();
    void showCreateFiringProgram();
    void showBatchProduction();
    void showPresintering();
}
