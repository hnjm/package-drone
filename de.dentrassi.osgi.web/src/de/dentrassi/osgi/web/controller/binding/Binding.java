/*******************************************************************************
 * Copyright (c) 2014 Jens Reimann.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jens Reimann - initial API and implementation
 *******************************************************************************/
package de.dentrassi.osgi.web.controller.binding;

public interface Binding
{
    public Object getValue ();

    public BindingResult getBindingResult ();

    public default Object postProcess ( final Object result )
    {
        return result;
    }

    // static factory methods

    public static Binding simpleBinding ( final Object value )
    {
        return new SimpleBinding ( value );
    }

    public static Binding nullBinding ()
    {
        return new Binding () {

            @Override
            public Object getValue ()
            {
                return null;
            }

            @Override
            public BindingResult getBindingResult ()
            {
                return null;
            }
        };
    }
}
