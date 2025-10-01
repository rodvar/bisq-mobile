import UIKit
import SwiftUI

import presentation
import domain

class LifecycleAwareComposeViewController: UIViewController {
    private let presenter: MainPresenter
    private let notificationServiceWrapper: NotificationServiceWrapper

    init(presenter: MainPresenter, notificationServiceWrapper: NotificationServiceWrapper) {
        self.presenter = presenter
        self.notificationServiceWrapper = notificationServiceWrapper
        GenericErrorHandler.companion.doInit()
        super.init(nibName: nil, bundle: nil)
    }

    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        NotificationCenter.default.removeObserver(self, name: UIApplication.didBecomeActiveNotification, object: nil)
        presenter.onPause()
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        presenter.onResume()
        notificationServiceWrapper.foregroundServiceController.stopService()
        NotificationCenter.default.addObserver(forName: UIApplication.willResignActiveNotification, object: nil, queue: .main) { [self] _ in
            notificationServiceWrapper.foregroundServiceController.startService()
            presenter.onPause()
        }
        NotificationCenter.default.addObserver(forName: UIApplication.didBecomeActiveNotification, object: nil, queue: .main) { [self] _ in
            notificationServiceWrapper.foregroundServiceController.stopService()
            presenter.onResume()
        }
    }

    @objc private func handleDidBecomeActive() {
        presenter.onResume()
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        presenter.onStart()
    }

    override func viewDidDisappear(_ animated: Bool) {
        super.viewDidDisappear(animated)
        presenter.onStop()
    }

    override func loadView() {
        super.loadView()

        // Instantiate the Kotlin-based MainViewController
        let mainViewController = MainViewControllerKt.MainViewController()
        // Add MainViewController as a child
        addChild(mainViewController)

        // Add the view of MainViewController to the current view
        view.addSubview(mainViewController.view)

        // Set up auto layout or frame if needed
        mainViewController.view.frame = view.bounds
        mainViewController.view.autoresizingMask = [.flexibleWidth, .flexibleHeight]

        // Notify the child view controller that it was moved to a parent
        mainViewController.didMove(toParent: self)
        presenter.attachView(view: self)
    }

    // Equivalent to `onDestroy` in Android for final cleanup
    deinit {
        presenter.onDestroy()
    }
}
