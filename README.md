# indef

Nrepl-middleware that exposes `:op "indef"` to inline def function inline arguments. They can be inspected direcly in the editor after instrumenting the defn form.


## General usage info

### middleware usage

Lein projects, put in `project.clj`

``` clojure
  :repl-options {:nrepl-middleware
               [indef.core/wrap-indef]}
```

`

### code calling the exposed operation
Here is an emacs snippet that can be used for the instrumentation
``` clojure
(defun instrument-form ()
  (interactive)
  (thread-first (cider-nrepl-send-request `("op"   "indef"
                                            "code" ,(cider-defun-at-point)
                                            "ns" ,(cider-current-ns))
                                          (lr-make-response-handler
                                           (lambda (_buffer value)
                                             (let ((value (if (zerop (length value)) "unlimited" value)))
                                               (message "form evaluated")))))))
```

## License

Copyright © 2024 FIXME

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
