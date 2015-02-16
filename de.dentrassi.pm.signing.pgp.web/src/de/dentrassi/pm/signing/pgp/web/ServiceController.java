/*******************************************************************************
 * Copyright (c) 2015 Jens Reimann.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jens Reimann - initial API and implementation
 *******************************************************************************/
package de.dentrassi.pm.signing.pgp.web;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.annotation.HttpConstraint;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.operator.bc.BcPBESecretKeyDecryptorBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPDigestCalculatorProvider;
import org.eclipse.scada.utils.ExceptionHelper;

import de.dentrassi.osgi.web.Controller;
import de.dentrassi.osgi.web.LinkTarget;
import de.dentrassi.osgi.web.ModelAndView;
import de.dentrassi.osgi.web.RequestMapping;
import de.dentrassi.osgi.web.RequestMethod;
import de.dentrassi.osgi.web.ViewResolver;
import de.dentrassi.osgi.web.controller.ControllerInterceptor;
import de.dentrassi.osgi.web.controller.binding.BindingResult;
import de.dentrassi.osgi.web.controller.binding.PathVariable;
import de.dentrassi.osgi.web.controller.form.FormData;
import de.dentrassi.osgi.web.controller.validator.ControllerValidator;
import de.dentrassi.osgi.web.controller.validator.ValidationContext;
import de.dentrassi.pm.common.web.CommonController;
import de.dentrassi.pm.common.web.InterfaceExtender;
import de.dentrassi.pm.common.web.Modifier;
import de.dentrassi.pm.common.web.menu.MenuEntry;
import de.dentrassi.pm.sec.web.controller.HttpContraintControllerInterceptor;
import de.dentrassi.pm.sec.web.controller.Secured;
import de.dentrassi.pm.sec.web.controller.SecuredControllerInterceptor;
import de.dentrassi.pm.signing.pgp.PgpHelper;

@Controller
@Secured
@ControllerInterceptor ( SecuredControllerInterceptor.class )
@HttpConstraint ( rolesAllowed = "ADMIN" )
@ControllerInterceptor ( HttpContraintControllerInterceptor.class )
@ViewResolver ( "/WEB-INF/views/%s.jsp" )
@RequestMapping ( "/pgp.sign" )
public class ServiceController implements InterfaceExtender
{
    private ServiceManager manager;

    public void setManager ( final ServiceManager manager )
    {
        this.manager = manager;
    }

    @Override
    public List<MenuEntry> getMainMenuEntries ( final HttpServletRequest request )
    {
        final List<MenuEntry> result = new LinkedList<> ();

        if ( request.isUserInRole ( "ADMIN" ) )
        {
            result.add ( new MenuEntry ( "Signing", 4_100, "PGP Signers", 1_000, LinkTarget.createFromController ( ServiceController.class, "index" ), null, null ) );
        }

        return result;
    }

    @Override
    public List<MenuEntry> getActions ( final HttpServletRequest request, final Object object )
    {
        final List<MenuEntry> result = new LinkedList<> ();

        if ( object == ServiceManager.ACTION_TAG_PGP )
        {
            if ( request.isUserInRole ( "ADMIN" ) )
            {
                result.add ( new MenuEntry ( "Add", 100, LinkTarget.createFromController ( ServiceController.class, "add" ), Modifier.PRIMARY, "plus" ) );
            }
        }

        return result;
    }

    @RequestMapping
    public ModelAndView index () throws Exception
    {
        final Map<String, Object> model = new HashMap<> ();

        model.put ( "services", this.manager.list () );

        return new ModelAndView ( "index", model );
    }

    @RequestMapping ( "/{id}/delete" )
    public ModelAndView delete ( @PathVariable ( "id" ) final String id )
    {
        try
        {
            this.manager.delete ( id );
        }
        catch ( final IOException e )
        {
            return CommonController.createError ( "Delete", "Failed to delete", e );
        }
        return new ModelAndView ( "redirect:/pgp.sign" );
    }

    @RequestMapping ( "/add" )
    public ModelAndView add ()
    {
        final Map<String, Object> model = new HashMap<> ();
        return new ModelAndView ( "add", model );
    }

    @RequestMapping ( value = "/add", method = RequestMethod.POST )
    public ModelAndView addPost ( @Valid @FormData ( "command" ) final AddEntry data, final BindingResult result )
    {
        final Map<String, Object> model = new HashMap<> ();

        if ( result.hasErrors () )
        {
            return new ModelAndView ( "add", model );
        }

        try
        {
            this.manager.add ( data );
        }
        catch ( final IOException e )
        {
            return CommonController.createError ( "Add PGP Signer", "Failed to add signer", e );
        }

        return new ModelAndView ( "redirect:/pgp.sign" );
    }

    @ControllerValidator ( formDataClass = AddEntry.class )
    public void validateAdd ( final AddEntry data, final ValidationContext context )
    {
        final String keyring = data.getKeyring ();
        final File file = new File ( keyring );

        if ( !file.exists () )
        {
            context.error ( "keyring", String.format ( "File '%s' does not exist on the server", file.getAbsolutePath () ) );
            return;
        }
        if ( !file.isFile () )
        {
            context.error ( "keyring", String.format ( "File '%s' is not a file", file.getAbsolutePath () ) );
            return;
        }
        if ( !file.canRead () )
        {
            context.error ( "keyring", String.format ( "File '%s' cannot be read", file.getAbsolutePath () ) );
            return;
        }

        final String keyId = data.getKeyId ();
        if ( keyId != null )
        {
            try
            {
                try ( InputStream input = new FileInputStream ( file ) )
                {
                    final PGPSecretKey key = PgpHelper.loadSecretKey ( input, keyId );
                    if ( key == null )
                    {
                        context.error ( "keyId", "Key not found in keyring" );
                    }
                    else if ( data.getKeyPassphrase () != null )
                    {
                        try
                        {
                            final PGPPrivateKey privateKey = key.extractPrivateKey ( new BcPBESecretKeyDecryptorBuilder ( new BcPGPDigestCalculatorProvider () ).build ( data.getKeyPassphrase ().toCharArray () ) );
                            if ( privateKey == null )
                            {
                                Thread.sleep ( 1_000 );
                                context.error ( "keyPassphrase", "Unable to unlock private key" );
                            }
                        }
                        catch ( final Exception e )
                        {
                            context.error ( "Failed to load key. Probably a wrong phassphrase: " + ExceptionHelper.getMessage ( e ) );
                        }
                    }
                }
            }
            catch ( final Exception e )
            {
                context.error ( "Failed to load key: " + ExceptionHelper.getMessage ( e ) );
            }
        }
    }
}
